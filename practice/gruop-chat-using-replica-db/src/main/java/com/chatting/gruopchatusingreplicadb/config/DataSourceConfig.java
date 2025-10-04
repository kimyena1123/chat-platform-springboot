package com.chatting.gruopchatusingreplicadb.config;

import com.chatting.gruopchatusingreplicadb.database.RoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * # DataSourceConfig (읽기/쓰기 분리용 데이터소스 구성)
 *
 * 이 설정은 다음 4가지를 만듭니다.
 *  1) sourceDataSource()  : "원본(Primary, 쓰기 가능)" 물리 데이터소스(HikariCP 풀)
 *  2) replicaDataSource() : "복제본(Replica, 읽기 전용)" 물리 데이터소스(HikariCP 풀)
 *  3) routingDataSource() : 상황에 따라 source/replica 중 하나를 선택하는 라우팅 데이터소스
 *  4) lazyConnectionDataSource() : 실제 쿼리 시점까지 커넥션 잡기를 지연시키는 프록시 (@Primary)
 *
 * 왜 이렇게 구성하나?
 *  - 읽기 쿼리는 복제본(Replica)으로 보내고, 쓰기 쿼리는 원본(Primary)로 보내 성능/확장성을 얻기 위함.
 *  - 실제 "어디로 보낼지" 결정은 RoutingDataSource.determineCurrentLookupKey() 결과값으로 한다.
 *    (예: 트랜잭션 readOnly 여부, AOP 컨텍스트, ThreadLocal 등을 활용)
 *
 * LazyConnectionDataSourceProxy를 왜 끼우나?
 *  - JPA/Hibernate는 트랜잭션 시작 시점에 연결을 미리 잡을 수 있는데,
 *    라우팅 키(읽기/쓰기)는 보통 메서드 진입 후에 결정된다.
 *  - 프록시를 앞에 두면 "실제 쿼리 실행 직전"에 커넥션을 획득하므로,
 *    올바른 라우팅 키가 정해진 뒤에 맞는 풀(source/replica)에서 연결을 가져오게 된다.
 */
@Slf4j
@Configuration
//Spring은 @Configuration 클래스들을 알파벳 순서로 로딩하는 경향이 있다.
// 그래서 명시적으로 order로 순서를 지정할 수 있다.
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE) // "트랜잭션 관리자는 다른 AOP 기반 Bean 설정들보다 가장 마지막에 적용되게 하라"
// Ordered.LOWEST_PRECEDENCE의 값은 Integer.MAX_VALUE이고,
// 즉 가장 낮은 우선순위 = 제일 마지막 실행을 의미한다.
public class DataSourceConfig {

    /**
     * 원본(Primary, 쓰기 가능) Hikari 데이터소스 빈.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.source.hikari")
    public DataSource sourceDataSource() {
        // HikariCP 풀을 사용하는 물리 DataSource 생성
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }


    /**
     * 복제본(Replica, 읽기 전용) Hikari 데이터소스 빈.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.replica.hikari")
    public DataSource replicaDataSource() {
        // HikariCP 풀을 사용하는 물리 DataSource 생성
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }


    /**
     * 라우팅 데이터소스
     *
     * - 내부적으로 "현재 컨텍스트의 라우팅 키" (예: "source" 또는 "replica")를 조회하여
     *   targetDataSources 에서 대응되는 물리 DataSource로 요청을 위임한다.
     * - setDefaultTargetDataSource: 라우팅 키가 없거나 오류인 경우 사용할 기본 대상(여기서는 source).
     *
     * 주의
     * - targetDataSource.put("source", ...) 의 키 문자열과
     *   RoutingDataSource.determineCurrentLookupKey() 가 반환하는 값이 정확히 일치해야 한다.
     *
     * 추가 로직: replica 풀 초기화 로그
     * - try-with-resources 로 replica.getConnection() 을 한 번 열었다 닫아
     *   "복제본 커넥션 풀이 실제로 초기화되었음"을 보장하고 로그를 남긴다.
     * - 장점: 부팅 시 replica 연결 이슈를 빨리 발견할 수 있음.
     * - 단점: replica 가 다운되어 있으면 애플리케이션도 같이 부팅 실패할 수 있음(의도된 fail-fast).
     *   → 환경에 따라 이 부분을 제거하거나 예외를 무시하도록 선택 가능.
     */
    @Bean
    public DataSource routingDataSource(
            @Qualifier("sourceDataSource") DataSource sourceDataSource,
            @Qualifier("replicaDataSource") DataSource replicaDataSource
    ) throws SQLException {
        // 우리가 구현한 AbstractRoutingDataSource 상속체
        RoutingDataSource routingDataSource = new RoutingDataSource();

        // 라우팅 키 -> 물리 DataSource 매핑
        Map<Object, Object> targetDataSource = new HashMap<>();
        targetDataSource.put("source", sourceDataSource);
        targetDataSource.put("replica", replicaDataSource);

        routingDataSource.setTargetDataSources(targetDataSource);
        routingDataSource.setDefaultTargetDataSource(sourceDataSource); // 키가 없거나 오류일 때 fallback

        // ★ 선택 사항(의도적 초기화) : replica 풀을 한 번 warm-up 하면서 로그를 남긴다.
        try(Connection connection = replicaDataSource.getConnection()){
          log.info("Init ReplicaConnectionPool."); // 성공적으로 커넥션을 얻었다는 의미
        }  // try-with-resources: 자동 close()

        return routingDataSource;
    }


    /**
     * LazyConnectionDataSourceProxy (JPA가 기본 사용하는 @Primary DataSource)
     *
     * - 이 프록시는 실제 쿼리 실행 시점에 커넥션을 획득한다.
     * - 그 순간의 라우팅 키(source/replica)에 따라 routingDataSource가 올바른 풀에서 커넥션을 가져온다.
     *
     * 왜 @Primary 를 여기에?
     * - 스프링/JPA는 DataSource 빈이 여럿일 때 어떤 것을 쓸지 모른다.
     * - 기본 데이터소스로 이 프록시를 지정하면, JPA/Hibernate가 항상 이 프록시를 통하게 되고
     *   결과적으로 라우팅+지연획득 전략이 안정적으로 적용된다.
     */
    @Primary // ★ 애플리케이션에서 "기본" 데이터소스로 사용하도록 지정
    @Bean
    public DataSource lazyConnectionDataSource(
            @Qualifier("routingDataSource") DataSource routingDataSource) {
        // 커넥션 지연 획득을 통해 "라우팅 결정 이후" 올바른 커넥션을 잡도록 보장
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}