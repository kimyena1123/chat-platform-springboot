package com.chatting.gruopchatusingreplicadb.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * # RoutingDataSource
 *
 * 읽기/쓰기 분리(Replica/Primary 라우팅)를 위해 **어떤 물리 DataSource로 커넥션을 빌릴지**를
 * 실행 시점에 결정하는 라우터입니다. 스프링이 제공하는 `AbstractRoutingDataSource`를 상속해
 * `determineCurrentLookupKey()`만 구현하면 됩니다.
 *
 * 동작 흐름(중요):
 *  1) 애플리케이션 코드가 JDBC 커넥션을 필요로 하는 시점(예: JPA가 쿼리 실행 직전)에
 *     `AbstractRoutingDataSource#getConnection()`이 호출됩니다.
 *  2) 그 내부에서 이 메서드 `determineCurrentLookupKey()`가 먼저 호출되어
 *     "현재 요청을 어느 풀(DataSource)로 보낼지"를 나타내는 **키(String)** 를 리턴해야 합니다.
 *  3) 이 키는 DataSource 설정(`DataSourceConfig`)에서
 *     `routingDataSource.setTargetDataSources(Map<Object,Object>)`에 등록한 키와 **정확히 일치**해야 합니다.
 *     - 예: "source" → Primary(쓰기), "replica" → Replica(읽기)
 *  4) 최종적으로 라우팅된 물리 DataSource에서 실제 DB 커넥션을 빌려와 쿼리가 수행됩니다.
 *
 * 이 구현의 라우팅 기준:
 *  - 스프링 트랜잭션의 **readOnly 여부**를 사용합니다.
 *  - `@Transactional(readOnly = true)` 이면 "replica" 로,
 *    그 외(없음/false)이면 "source" 로 라우팅합니다.
 *
 * 왜 readOnly로 판단하나?
 *  - 서비스/리포지토리 메서드에 이미 존재하는 표준 어노테이션만으로
 *    "읽기 쿼리는 복제본, 쓰기는 원본" 이라는 정책을 자연스럽게 적용할 수 있기 때문입니다.
 *
 * 라우팅이 올바르게 작동하려면?
 *  - **지연 커넥션 획득(LazyConnectionDataSourceProxy)** 를 반드시 DataSource 앞단에 둬야 합니다.
 *    그래야 트랜잭션이 시작되고(readOnly 여부가 결정된 뒤) 실제 커넥션을 얻기 직전에
 *    이 메서드가 호출되어 올바른 대상(source/replica)을 선택할 수 있습니다.
 *    (설정은 `DataSourceConfig.lazyConnectionDataSource()`에서 수행)
 *
 * 주의 사항:
 *  - 트랜잭션이 **없으면** `isCurrentTransactionReadOnly()` 는 false → 기본값으로 "source" 선택됩니다.
 *    → 트랜잭션 없이 읽기만 하는 코드도 replica를 쓰고 싶다면,
 *       해당 메서드에 `@Transactional(readOnly = true)` 를 붙이거나,
 *       별도의 ThreadLocal 컨텍스트를 만들어 강제로 키를 지정하는 확장을 고려하세요.
 *  - 실제 Replica는 MySQL에서 `read_only=ON` 등으로 설정되어 있어야 **실수로 쓰기가 들어가는 것을 방지**할 수 있습니다.
 *  - Replica 지연(lag)으로 인한 **강한 일관성**이 필요한 경우(방금 쓴 데이터를 반드시 읽어야 하는 경우)는
 *    `@Transactional(readOnly = false)` 또는 별도 강제 Master 라우팅을 통해 "source"로 보내야 합니다.
 */
@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {

    //이 메서드는 connection을 맺기 전에 호출된다 : 어떤 데이터 소스에서 커넥션을 얻어서 어느 DB로 갈건지 찾는 과정에서 호출되는 메서드
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceKey = TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? "replica" : "source";
        log.info("Routing to {} DataSource. ", dataSourceKey);

        return dataSourceKey;
    }
}
