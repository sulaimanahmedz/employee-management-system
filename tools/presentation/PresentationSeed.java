import com.github.javafaker.Faker;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
class PresentationSeed {
  static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""; }
  public static void main(String[] args) throws Exception {
    if (!"true".equals(System.getenv("PRESENTATION_SEED"))) throw new IllegalStateException("Presentation opt-in required");
    var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    String base = "http://127.0.0.1:8080/api/v1/employees";
    var existing = client.send(HttpRequest.newBuilder(URI.create(base)).GET().build(), HttpResponse.BodyHandlers.ofString());
    if (existing.statusCode()!=200 || !existing.body().trim().equals("[]")) throw new IllegalStateException("Requires an empty isolated presentation database");
    Faker faker = new Faker(Locale.UK, new Random(20261025));
    String[] departments={"Engineering","People Operations","Finance","Customer Success","Operations"};
    String[] statuses={"ACTIVE","ACTIVE","ON_LEAVE","ACTIVE","INACTIVE"};
    for(int i=0;i<5;i++) {
      String payload="{\"firstName\":"+quote(faker.name().firstName())+",\"lastName\":"+quote(faker.name().lastName())+",\"email\":"+quote("presentation."+i+"@example.com")+",\"department\":"+quote(departments[i])+",\"jobTitle\":"+quote(faker.job().title())+",\"status\":"+quote(statuses[i])+"}";
      var result=client.send(HttpRequest.newBuilder(URI.create(base)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build(),HttpResponse.BodyHandlers.ofString());
      if(result.statusCode()!=201) throw new IllegalStateException("Synthetic seed failed: HTTP "+result.statusCode());
    }
    System.out.println("Java Faker seeded five synthetic employees before presentation startup.");
  }
}
