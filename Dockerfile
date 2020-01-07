FROM maven

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["sh", "-c", "java -jar /app.jar $MAIL_SERVER $MAIL_USER $MAIL_PASSWORD" ]
