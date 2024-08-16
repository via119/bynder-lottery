FROM ubuntu:latest

RUN apt-get update && apt-get install -y cron curl

COPY close_lotteries.sh /usr/local/bin/close_lotteries.sh

COPY crontab.txt /etc/cron.d/mycron

RUN chmod 0644 /etc/cron.d/mycron

RUN crontab /etc/cron.d/mycron

RUN touch /var/log/cron.log

CMD cron && tail -f /var/log/cron.log