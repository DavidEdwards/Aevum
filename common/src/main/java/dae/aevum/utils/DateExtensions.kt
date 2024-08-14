package dae.aevum.utils

import java.time.Duration

fun Duration.human(): String {
    return toString()
        .substring(2)
        .replace("(\\d[HMS])(?!$)".toRegex(), "$1 ")
        .replace("(\\d+)\\.\\d+(\\D)".toRegex(), "$1$2")
        .lowercase()
}