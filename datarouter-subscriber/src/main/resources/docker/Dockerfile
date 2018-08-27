FROM java:8
ADD opt /opt/
ADD startup.sh /startup.sh
RUN chmod 700 /startup.sh
ENTRYPOINT ./startup.sh start
EXPOSE 7070