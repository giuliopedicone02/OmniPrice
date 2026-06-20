package com.unict.dmi.omniprice.cluster;

/**
 * Stati possibili di un nodo nel pattern Leader and Followers (ISD §2.2.3).
 */
public enum NodeState {
    /** Coordina la replicazione e prende le decisioni per il cluster. */
    LEADER,
    /** Segue il leader; gli inoltra le richieste dei client. */
    FOLLOWER,
    /** Sta cercando/contendendo la leadership durante un'elezione. */
    CANDIDATE
}
