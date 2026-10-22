# Harness Engineering for Java Developers

## Spring Javaformat
* https://github.com/spring-io/spring-javaformat
* https://maven.apache.org/plugins/maven-checkstyle-plugin/

```xml
<build>
	<plugins>
		<plugin>
			<groupId>io.spring.javaformat</groupId>
			<artifactId>spring-javaformat-maven-plugin</artifactId>
			<version>0.0.48</version>
			<executions>
				<execution>
					<phase>validate</phase>
					<inherited>true</inherited>
					<goals>
						<goal>validate</goal>
					</goals>
				</execution>
			</executions>
		</plugin>
	</plugins>
</build>
```

Format code: `./mvnw spring-javaformat:apply`

## Spotless Maven Plugin

* https://github.com/diffplug/spotless
* https://github.com/palantir/palantir-java-format

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>${spotless.version}</version>
    <configuration>
        <java>
            <importOrder/>
            <removeUnusedImports/>
            <formatAnnotations/>
            <palantirJavaFormat>
                <version>${palantir-java-format.version}</version>
            </palantirJavaFormat>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
            <phase>compile</phase>
        </execution>
    </executions>
</plugin>
```

## JaCoCo Code Coverage

* https://www.eclemma.org/jacoco/trunk/doc/maven.html
* https://docs.gradle.org/current/userguide/jacoco_plugin.html

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco-maven-plugin.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <goals>
                <goal>report</goal>
            </goals>
            <phase>verify</phase>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <phase>verify</phase>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>${jacoco.minimum.coverage}</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Arch Unit

* https://www.archunit.org/

```java
@AnalyzeClasses(packages = "dev.sivalabs.feedbackhub")
class ArchUnitTests {

    @ArchTest
    static final ArchRule repositoriesShouldNotBePublic =
            classes().that()
                .areAssignableTo(Repository.class)
                .should().notBePublic();

    @ArchTest
    static final ArchRule entitiesShouldNotBePublic =
            classes().that()
                .areAnnotatedWith(Entity.class)
                .should().notBePublic();

    @ArchTest
    static final ArchRule controllersShouldNotBeAccessedByDomain = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .accessClassesThat()
            .resideInAPackage("..web..");

    @ArchTest
    static final ArchRule shouldFollowLayeredArchitecture = 
            layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Persistence").definedBy("..persistence..")
            
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service");
        
    @ArchTest
    static final ArchRule shouldFollowOnionArchitecture = 
            onionArchitecture()
                .domainModels("com.myapp.domain.model..")
                .domainServices("com.myapp.domain.service..")
                .applicationServices("com.myapp.application..")
                .adapter("cli", "com.myapp.adapter.cli..")
                .adapter("persistence", "com.myapp.adapter.persistence..")
                .adapter("rest", "com.myapp.adapter.rest..");
    
    
}
```

## Taikai 

* https://github.com/enofex/taikai

```java
class TaikaiTests {
    private static final String BASE_PACKAGE = TaikaiTests.class.getPackageName();

    @Test
    void shouldFulfillConstraints() {
        Taikai.builder()
                .namespace(BASE_PACKAGE)
                .java(java -> java.noUsageOfDeprecatedAPIs()
                    .methodsShouldNotDeclareGenericExceptions()
                    .utilityClassesShouldBeFinalAndHavePrivateConstructor()
                    .imports(ImportsConfigurer::shouldHaveNoCycles)
                    .naming(naming -> naming.fieldsShouldNotMatch(".*(List|Set|Map)$")
                        .constantsShouldFollowConventions()
                        .interfacesShouldNotHavePrefixI()))
                .logging(logging ->
                    logging.loggersShouldFollowConventions(Logger.class, "LOG", List.of(PRIVATE, STATIC, FINAL)))
                .test(test -> test.junit(
                    junit -> junit.classesShouldBePackagePrivate(ClassCriteria.simpleNameMatching(".*Test(s)"))
                    .classesShouldNotBeAnnotatedWithDisabled()
                    .methodsShouldNotBeAnnotatedWithDisabled()))
                .jpa(jpa -> jpa.entities(entities -> entities.shouldBePackagePrivate()
                    .idsShouldNotBeOfType(ClassCriteria.type(UUID.class))
                    .shouldHaveFieldAnnotatedWithVersion()
                    .fieldsShouldNotBeOfType(ClassCriteria.type(Date.class)
                        .or(ClassCriteria.assignableTo(Date.class))
                        .or(ClassCriteria.type(Calendar.class))
                        .or(ClassCriteria.assignableTo(Calendar.class)))
                    .namesShouldMatch(".+Entity")))
                .spring(spring -> spring.noAutowiredFields()
                    .boot(boot -> boot.applicationClassShouldResideInPackage(BASE_PACKAGE))
                    .configurations(c -> c.namesShouldMatch(".+Config"))
                    .controllers(controllers -> controllers
                            .namesShouldEndWithController()
                            .shouldNotDependOnOtherControllers()
                            .shouldBePackagePrivate())
                    .services(services -> services.shouldBeAnnotatedWithService()
                            .shouldNotDependOnControllers()
                            .namesShouldEndWithService())
                    .repositories(repositories -> repositories
                            .shouldNotDependOnServices()
                            .namesShouldEndWithRepository()
                            .shouldBePackagePrivate())
                    .transactional(transactional ->
                            transactional.methodsShouldBePublic().shouldNotBeUsedInControllers()))
                .build()
                .checkAll();
    }
}
```

## Spring Modulith

* https://spring.io/projects/spring-modulith

```java
class ModularityTests {
    private final ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void verifyModuleStructure() {
        modules.verify();
    }
}
```
