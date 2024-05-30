/**
 * System              : Genesis Business Library
 * Sub-System          : multi-pro-code-test Configuration
 * Version             : 1.0
 * Copyright           : (c) Genesis
 * Date                : 2022-03-18
 * Function : Provide view config for multi-pro-code-test.
 *
 * Modification History
 */
views {
    view("SYMPHONY_ROOM_ROUTE", NOTIFY_ROUTE) {
        joins {
            joining(SYMPHONY_ROOM_NOTIFY_ROUTE_EXT, joinType = JoinType.INNER) {
                on(NOTIFY_ROUTE.NOTIFY_ROUTE_ID  to SYMPHONY_ROOM_NOTIFY_ROUTE_EXT.NOTIFY_ROUTE_ID)
            }
        }
        fields {
            NOTIFY_ROUTE.except(listOf(NOTIFY_ROUTE.RECORD_ID, NOTIFY_ROUTE.TIMESTAMP))
            SYMPHONY_ROOM_NOTIFY_ROUTE_EXT.except(
                listOf(
                    SYMPHONY_ROOM_NOTIFY_ROUTE_EXT.NOTIFY_ROUTE_ID,
                    SYMPHONY_ROOM_NOTIFY_ROUTE_EXT.RECORD_ID,
                    SYMPHONY_ROOM_NOTIFY_ROUTE_EXT.TIMESTAMP
                )
            )
        }
    }
    view("SYMPHONY_USER_ROUTE", NOTIFY_ROUTE) {
        joins {
            joining(SYMPHONY_BY_USER_EMAIL_NOTIFY_ROUTE_EXT, joinType = JoinType.INNER) {
                on(NOTIFY_ROUTE.NOTIFY_ROUTE_ID to SYMPHONY_BY_USER_EMAIL_NOTIFY_ROUTE_EXT.NOTIFY_ROUTE_ID)
            }
        }
        fields {
            NOTIFY_ROUTE.except(listOf(NOTIFY_ROUTE.RECORD_ID, NOTIFY_ROUTE.TIMESTAMP))
            SYMPHONY_BY_USER_EMAIL_NOTIFY_ROUTE_EXT.except(
                listOf(
                    SYMPHONY_BY_USER_EMAIL_NOTIFY_ROUTE_EXT.NOTIFY_ROUTE_ID,
                    SYMPHONY_BY_USER_EMAIL_NOTIFY_ROUTE_EXT.RECORD_ID,
                    SYMPHONY_BY_USER_EMAIL_NOTIFY_ROUTE_EXT.TIMESTAMP
                )
            )
        }
    }
}
