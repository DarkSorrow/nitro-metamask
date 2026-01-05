package com.nitrometamask

import android.content.Context

object MetamaskContextHolder {
    private var context: Context? = null

    fun initialize(ctx: Context) {
        context = ctx.applicationContext
    }

    fun get(): Context {
        return context ?: throw IllegalStateException("Context not initialized")
    }
}
