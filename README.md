# Bynder lottery service


## Requiremenst
- sbt
- docker

## How to run
- build the docker image of the scala service using sbt: `sbt lotteryService/docker:publishLocal`
- start the containers with docker-compose

### Supported endpoints

- `POST /user`  
Creates a new user. A new id is generated for every users. This id can be used to entry a lottery.  
Example JSON payload:
```
{
  "first_name": "John",
  "last_name": "Smith",
  "email": "john.smith@gmail.com"
}
```
- `POST /entry`  
Creates a new entry for the selected user for the selected lottery. It returns the entry id. This id can be used to identify the winner.
Example JSON payload:
```
{
  "participant_id": 1,
  "lottery_id": 1
}
```
- `GET /lottery`  
  Returns every lottery (both active and closed)
- `POST /lottery/close`  
  Closes every active lottery and chooses a winner for them. It returns the winner entry for each active lottery.
- `POST /lottery/create`  
  Creates a new lottery. It returns the id of the new lottery.  
  Example JSON payload:
```
{
  "name": "new lottery"
}
```
- `GET /winner`  
  Returns the winner entry id for all lotteries for the specified date.
  Example JSON payload:
```
{
  "date": "2024-08-17"
}
```

### Details
- the lottery service is running on port `28080`
- the service starts with one running lottery with id `1`
- there is a cron job which closes every active lottery at midnight
- everything is handled in UTC
