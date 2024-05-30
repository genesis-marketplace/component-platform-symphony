/**
 * System              : Genesis Business Library
 * Sub-System          : multi-pro-code-test Configuration
 * Version             : 1.0
 * Copyright           : (c) Genesis
 * Date                : 2022-03-18
 * Function : Provide dataserver config for multi-pro-code-test.
 *
 * Modification History
 */
dataServer {
    query("ALL_SYMPHONY_ROOM_ROUTES", SYMPHONY_ROOM_ROUTE) {
        permissioning {
            permissionCodes = listOf("NOTIFY_ROUTE_VIEW")
        }
    }
    query("ALL_SYMPHONY_USER_ROUTES", SYMPHONY_USER_ROUTE) {
        permissioning {
            permissionCodes = listOf("NOTIFY_ROUTE_VIEW")
        }
    }
}
