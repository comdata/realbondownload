[![Gitpod Ready-to-Code](https://img.shields.io/badge/Gitpod-Ready--to--Code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/comdata/realbondownload) 

# realbondownload

for the moment just PoC code

mail download working


docker-compose.yml:

  realbondownload:
    image: comdata456/realbondownload
    container_name: realbondownload
    environment:
      - MAIL_SERVER=imap.gmail.com
      - MAIL_USER=<mail address>
      - MAIL_PASSWORD=<password>
    links:
      - "ha-mariadb"  # database server, expects database called HA using MariaDB
    networks:
      - homeautomation   # shared network with the database
