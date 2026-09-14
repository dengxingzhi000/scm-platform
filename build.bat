set JAVA_HOME=C:\Users\Deng\.jdks\graalvm-jdk-21.0.8
set PATH=%JAVA_HOME%\bin;%PATH%
call "C:\Users\Deng\.m2\wrapper\dists\apache-maven-3.9.16-bin\6h7i8j9k0l1m2n3o4p5q6r7s8t9\apache-maven-3.9.16\bin\mvn.cmd" clean compile -f com.scm.parent/pom.xml -DskipTests
