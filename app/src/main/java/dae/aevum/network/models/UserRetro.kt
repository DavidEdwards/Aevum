package dae.aevum.network.models

import androidx.annotation.Keep

@Keep
data class UserResponseRetro(
    val users: List<UserRetro>,
    val total: Int,
    val header: String
)

@Keep
data class UserRetro(
    val accountId: String,
    val accountType: String,
    val html: String,
    val displayName: String
)

//{
//    "users": [
//    {
//        "accountId": "1234567890",
//        "accountType": "atlassian",
//        "html": "Person Name - <strong>person.name@domain.com</strong>",
//        "displayName": "Person Name"
//    }
//    ],
//    "total": 1,
//    "header": "Showing 1 of 1 matching users"
//}