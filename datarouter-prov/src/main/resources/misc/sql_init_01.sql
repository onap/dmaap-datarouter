use datarouter;

CREATE TABLE FEEDS (
    FEEDID         INT UNSIGNED NOT NULL PRIMARY KEY,
    GROUPID        INT(10) UNSIGNED NOT NULL DEFAULT 0,
    NAME           VARCHAR(255) NOT NULL,
    VERSION        VARCHAR(20) NOT NULL,
    DESCRIPTION    VARCHAR(1000),
    BUSINESS_DESCRIPTION VARCHAR(1000) DEFAULT NULL,
    AUTH_CLASS     VARCHAR(32) NOT NULL,
    PUBLISHER      VARCHAR(8) NOT NULL,
    SELF_LINK      VARCHAR(256),
    PUBLISH_LINK   VARCHAR(256),
    SUBSCRIBE_LINK VARCHAR(256),
    LOG_LINK       VARCHAR(256),
    DELETED        BOOLEAN DEFAULT FALSE,
    LAST_MOD       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    SUSPENDED      BOOLEAN DEFAULT FALSE,
    CREATED_DATE   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE FEED_ENDPOINT_IDS (
    FEEDID        INT UNSIGNED NOT NULL,
    USERID        VARCHAR(20) NOT NULL,
    PASSWORD      VARCHAR(32) NOT NULL
);

CREATE TABLE FEED_ENDPOINT_ADDRS (
    FEEDID        INT UNSIGNED NOT NULL,
    ADDR          VARCHAR(44) NOT NULL
);

CREATE TABLE SUBSCRIPTIONS (
    SUBID                       INT UNSIGNED NOT NULL PRIMARY KEY,
    FEEDID                      INT UNSIGNED NOT NULL,
    GROUPID                     INT(10) UNSIGNED NOT NULL DEFAULT 0,
    DELIVERY_URL                VARCHAR(256),
    DELIVERY_USER               VARCHAR(20),
    DELIVERY_PASSWORD           VARCHAR(32),
    DELIVERY_USE100             BOOLEAN DEFAULT FALSE,
    METADATA_ONLY               BOOLEAN DEFAULT FALSE,
    SUBSCRIBER                  VARCHAR(8) NOT NULL,
    SELF_LINK                   VARCHAR(256),
    LOG_LINK                    VARCHAR(256),
    LAST_MOD                    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    SUSPENDED                   BOOLEAN DEFAULT FALSE,
    PRIVILEGED_SUBSCRIBER       BOOLEAN DEFAULT FALSE,
    CREATED_DATE                TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

CREATE TABLE PARAMETERS (
    KEYNAME        VARCHAR(32) NOT NULL PRIMARY KEY,
    VALUE          VARCHAR(4096) NOT NULL
);

CREATE TABLE LOG_RECORDS (
    TYPE           ENUM('pub', 'del', 'exp', 'pbf', 'dlx') NOT NULL,
    EVENT_TIME     BIGINT NOT NULL,           /* time of the publish request */
    PUBLISH_ID     VARCHAR(64) NOT NULL,      /* unique ID assigned to this publish attempt */
    FEEDID         INT UNSIGNED NOT NULL,     /* pointer to feed in FEEDS */
    REQURI         VARCHAR(256) NOT NULL,     /* request URI */
    METHOD         ENUM('DELETE', 'GET', 'HEAD', 'OPTIONS', 'PUT', 'POST', 'TRACE') NOT NULL, /* HTTP method */
    CONTENT_TYPE   VARCHAR(256) NOT NULL,     /* content type of published file */
    CONTENT_LENGTH BIGINT NOT NULL,  /* content length of published file */

    FEED_FILEID    VARCHAR(256),        /* file ID of published file */
    REMOTE_ADDR    VARCHAR(40),         /* IP address of publishing endpoint */
    USER           VARCHAR(50),         /* user name of publishing endpoint */
    STATUS         SMALLINT,            /* status code returned to delivering agent */

    DELIVERY_SUBID INT UNSIGNED,        /* pointer to subscription in SUBSCRIPTIONS */
    DELIVERY_FILEID  VARCHAR(256),      /* file ID of file being delivered */
    RESULT         SMALLINT,            /* result received from subscribing agent */

    ATTEMPTS       INT,             /* deliveries attempted */
    REASON         ENUM('notRetryable', 'retriesExhausted', 'diskFull', 'other'),

    RECORD_ID      BIGINT UNSIGNED NOT NULL PRIMARY KEY, /* unique ID for this record */
    CONTENT_LENGTH_2 BIGINT,

    INDEX (FEEDID) USING BTREE,
    INDEX (DELIVERY_SUBID) USING BTREE,
    INDEX (RECORD_ID) USING BTREE
) ENGINE = MyISAM;

CREATE TABLE INGRESS_ROUTES (
    SEQUENCE  INT UNSIGNED NOT NULL,
    FEEDID    INT UNSIGNED NOT NULL,
    USERID    VARCHAR(20),
    SUBNET    VARCHAR(44),
    NODESET   INT UNSIGNED NOT NULL
);

CREATE TABLE EGRESS_ROUTES (
    SUBID    INT UNSIGNED NOT NULL PRIMARY KEY,
    NODEID   INT UNSIGNED NOT NULL
);

CREATE TABLE NETWORK_ROUTES (
    FROMNODE INT UNSIGNED NOT NULL,
    TONODE   INT UNSIGNED NOT NULL,
    VIANODE  INT UNSIGNED NOT NULL
);

CREATE TABLE NODESETS (
    SETID   INT UNSIGNED NOT NULL,
    NODEID  INT UNSIGNED NOT NULL
);

CREATE TABLE NODES (
    NODEID  INT UNSIGNED NOT NULL PRIMARY KEY,
    NAME    VARCHAR(255) NOT NULL,
    ACTIVE  BOOLEAN DEFAULT TRUE
);

CREATE TABLE GROUPS (
    GROUPID        INT UNSIGNED NOT NULL PRIMARY KEY,
    AUTHID         VARCHAR(100) NOT NULL,
    NAME           VARCHAR(50) NOT NULL,
    DESCRIPTION    VARCHAR(255),
    CLASSIFICATION VARCHAR(20) NOT NULL,
    MEMBERS        TINYTEXT,
    LAST_MOD       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO PARAMETERS VALUES
    ('ACTIVE_POD',  'dmaap-dr-prov'),
    ('PROV_ACTIVE_NAME',  'dmaap-dr-prov'),
    ('STANDBY_POD', ''),
    ('PROV_NAME',   'dmaap-dr-prov'),
    ('NODES',       'dmaap-dr-node'),
    ('PROV_DOMAIN', ''),
    ('DELIVERY_INIT_RETRY_INTERVAL', '10'),
    ('DELIVERY_MAX_AGE', '86400'),
    ('DELIVERY_MAX_RETRY_INTERVAL', '3600'),
    ('DELIVERY_FILE_PROCESS_INTERVAL', '600'),
    ('DELIVERY_RETRY_RATIO', '2'),
    ('LOGROLL_INTERVAL', '300'),
    ('PROV_AUTH_ADDRESSES', 'dmaap-dr-prov|dmaap-dr-node'),
    ('PROV_AUTH_SUBJECTS', ''),
    ('PROV_MAXFEED_COUNT',  '10000'),
    ('PROV_MAXSUB_COUNT',   '100000'),
    ('PROV_REQUIRE_CERT', 'false'),
    ('PROV_REQUIRE_SECURE', 'false'),
    ('_INT_VALUES', 'LOGROLL_INTERVAL|PROV_MAXFEED_COUNT|PROV_MAXSUB_COUNT|DELIVERY_INIT_RETRY_INTERVAL|DELIVERY_MAX_RETRY_INTERVAL|DELIVERY_RETRY_RATIO|DELIVERY_MAX_AGE|DELIVERY_FILE_PROCESS_INTERVAL')
    ;
