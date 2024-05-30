/**
 * System              : Genesis Business Library
 * Sub-System          : multi-pro-code-test Configuration
 * Version             : 1.0
 * Copyright           : (c) Genesis
 * Date                : 2022-03-18
 * Function : Provide table definition config for multi-pro-code-test.
 *
 * Modification History
 */

tables {
    table(name = "SYMPHONY_ROOM_NOTIFY_ROUTE_EXT", id = 34, audit = details(35, "SR")) {
        NOTIFY_ROUTE_ID
        ROOM_ID not null
        primaryKey {
            NOTIFY_ROUTE_ID
        }
    }
    table(name = "SYMPHONY_BY_USER_EMAIL_NOTIFY_ROUTE_EXT", id = 45, audit = details(46, "SB")) {
        NOTIFY_ROUTE_ID
        ENTITY_ID
        ENTITY_ID_TYPE not null
        AUTH_CACHE_NAME
        RIGHT_CODE
        EXCLUDE_SENDER
        primaryKey {
            NOTIFY_ROUTE_ID
        }
    }
}
