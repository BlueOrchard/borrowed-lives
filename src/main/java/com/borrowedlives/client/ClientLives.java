package com.borrowedlives.client;

import com.borrowedlives.network.LivesPayload;

/**
 * The local player's lives as last sent by the server. Holds no client-only
 * classes, so it is safe to reference from the payload handler on a dedicated server.
 */
public final class ClientLives {
    public static volatile int lives = LivesPayload.INACTIVE;

    private ClientLives() {}
}
