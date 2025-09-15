FROM openjdk:17-jdk-slim

WORKDIR /usrapp/bin

ENV PORT 35000
ENV THREAD_POOL_SIZE 20
ENV CONTEXT_PATH ""
ENV ENVIRONMENT "production"

COPY target/classes /usrapp/bin/classes
COPY target/dependency /usrapp/bin/dependency
COPY src/main/resources /usrapp/bin/resources

CMD ["java", "-cp", "./classes:./dependency/*", "edu.escuelaing.arem.ASE.app.MicroSpringBoot", "edu.escuelaing.arem.ASE.app.Controller"]