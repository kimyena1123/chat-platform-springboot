import com.study.preview_websocket.PreviewWebsocketApplication
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification;

@SpringBootTest(classes = PreviewWebsocketApplication)
class PreviewWebsocketApplicationSpec extends Specification{

    void contextLoads() {
        expect:
        true
    }

}
