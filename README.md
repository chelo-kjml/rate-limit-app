# Rate limit app

---

Rate limit implementation for Modak's technical task.

## Prerequisites

- Mysql Database 
- Run ddl:
```mysql
create database rate_limiter;

use rate_limiter;

create table rate_limit_type(
    id bigint not null primary key auto_increment,
    name varchar(45) not null unique,
    time_window_in_seconds int not null,
    max_threshold int
);

create table user_operation(
    user_id varchar(140) not null,
    rate_limit_type_id bigint not null,
    inserted_at timestamp(6) not null default current_timestamp(6),
    primary key (user_id, rate_limit_type_id, inserted_at)
);
```
- Application Configuration File:
```yaml
common.datasource:
  poolName: mysql-pool
  jdbcUrl: jdbc:mysql://localhost:3306/rate_limiter
  username: { your_user } 
  password: { your_password } 
  connection-test-query: SELECT 1
  minimum-idle: 10
  validation-timeout: 30000
  register-mbeans: false
  maximum-pool-size: 15
```

## Testing

---
This application uses test-containers (https://www.testcontainers.org/) library for running tests against a mysql db container.
A docker daemon must be running while testing.

## About this task

---

- Although an in memory rate limit implementation (such as https://bucket4j.com/) would work for this practical exercise,
considering a more realistic scenario persistence into disk is necessary to avoid losing operation tracking after deployments or downtimes.

- Considering a concurrent scenario, I opted for using DB locks instead of code locks considering this service would be a micro service with many running instances at the same time.

## Possible improvements

---

- A scheduled task that moves old operations into another table to reduce data volume so count operation can work faster.

- Depending on the volume of data, an in-memory table such as rowstore tables from singlestore db could be used moving old data periodically into a columnar table into disk to reduce data volume for count operation.