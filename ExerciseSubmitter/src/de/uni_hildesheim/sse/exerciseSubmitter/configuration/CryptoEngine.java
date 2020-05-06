package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

/**
 * Defines a wrapper class for a (symmetric) encryption of strings.
 * 
 * @author Alexander Schmehl
 * @since 1.00
 * @version 2.00
 */
public class CryptoEngine {

    /**
     * Defines the crypto engine to use.
     * 
     * @since version 2.00
     */
    private static CryptoEngine engine = new CryptoEngineImplCaesar(6);

    /**
     * Returns the current crypto engine.
     * 
     * @return the crypto engine instance
     * 
     * @since version 2.00
     */
    public static final CryptoEngine getInstance() {
        return engine;
    }

    /**
     * Encrypts a given string.
     * 
     * @param toEncrypt
     *            the string to encrypt
     * 
     * @return the encrypted string
     * 
     * @since version 1.00
     */
    public String encrypt(String toEncrypt) {
        return engine.encrypt(toEncrypt);
    }

    /**
     * Decrypts a given encrypted string.
     * 
     * @param toDecrypt
     *            the encrypted string to be decrypted
     * 
     * @return the decrypted string
     * 
     * @since version 1.00
     */
    public String decrypt(String toDecrypt) {
        return engine.decrypt(toDecrypt);
    }

}
