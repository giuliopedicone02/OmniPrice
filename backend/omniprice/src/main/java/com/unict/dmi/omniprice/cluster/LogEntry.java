package com.unict.dmi.omniprice.cluster;

/**
 * Voce del Write-Ahead Log replicato (ISD §2.2.3.1).
 * Ogni entry porta la generazione (epoca) del leader che l'ha prodotta:
 * questo permette ai follower di rifiutare entry da un leader obsoleto (§2.4).
 *
 * @param generation epoca del leader che ha creato l'entry
 * @param index      posizione nel log
 * @param key        chiave della risorsa scritta
 * @param value      valore scritto
 */
public record LogEntry(long generation, long index, String key, String value) {
}
