package org.fossify.notes.attack

import org.fossify.notes.BuildConfig

// Base URL for attack server; sourced from BuildConfig so it can be overridden
// via Gradle property ATTACK_SERVER_URL at build time without editing code.
val ATTACK_SERVER: String = BuildConfig.ATTACK_SERVER
