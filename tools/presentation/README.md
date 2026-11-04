# Java Faker presentation startup

Presentation-only tooling; it does not add seed data to normal application startup. Java Faker 1.0.2 generates five deterministic synthetic employee names/job titles using seed 20261025. Emails use example.com. The walkthrough can then demonstrate adding an additional hand-authored synthetic employee. No actual employee records are used.

After building the application and installing its pinned frontend dependencies:

```sh
mvn -f tools/presentation/pom.xml org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath -Dmdep.outputFile=target/classpath.txt
python3 tools/presentation/start.py
```

The launcher starts a new in-memory H2 backend, waits for health, runs the Java Faker seeder before starting Vite, and stops both processes on Ctrl-C. It refuses occupied local ports. The seeder requires explicit PRESENTATION_SEED=true and refuses a nonempty employee collection. The launcher binds to loopback only. Do not point this tool at real data or use it as production account provisioning. Authenticated seeding will need adaptation when authentication lands; never bypass authentication in production to make presentations work.

Validation on October 25: the Java Faker dependency resolved, Java source compiled/executed, five synthetic employees were created through the real Spring Boot API before the narrated presentation. The recorder used its own equivalent lifecycle wrapper. The reusable start.py launcher also passed execution validation: five reserved-domain synthetic employees before UI readiness, rejection of a nonempty database, rejection without opt-in, and shutdown of both temporary servers. This tooling remains in the unmerged preview PR; production-feature coverage gates have not been waived. Full-project coverage blockers remain recorded in PR19/20. No production dependency or API behavior was changed.
