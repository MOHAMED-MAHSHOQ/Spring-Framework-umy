drop table if exists beer_order_line cascade;
drop table if exists beer_order cascade;

CREATE TABLE beer_order (
                            id                 varchar(36) NOT NULL,
                            created_date       timestamp DEFAULT NULL,
                            customer_ref       varchar(255) DEFAULT NULL,
                            last_modified_date timestamp DEFAULT NULL,
                            version            bigint DEFAULT NULL,
                            customer_id        varchar(36) DEFAULT NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT customer_id_fk FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE TABLE beer_order_line (
                                 id                 varchar(36) NOT NULL,
                                 beer_id            varchar(36) DEFAULT NULL,
                                 created_date       timestamp DEFAULT NULL,
                                 last_modified_date timestamp DEFAULT NULL,
                                 order_quantity     int DEFAULT NULL,
                                 quantity_allocated int DEFAULT NULL,
                                 version            bigint DEFAULT NULL,
                                 beer_order_id      varchar(36) DEFAULT NULL,
                                 PRIMARY KEY (id),
                                 CONSTRAINT beer_order_id_fk FOREIGN KEY (beer_order_id) REFERENCES beer_order (id),
                                 CONSTRAINT beer_id_fk FOREIGN KEY (beer_id) REFERENCES beer (id)
);