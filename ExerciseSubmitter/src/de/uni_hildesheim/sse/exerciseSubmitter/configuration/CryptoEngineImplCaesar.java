package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

/**
 * A simple crypto engine implementing the symmetrical caesar algorithm.
 * 
 * @author Maria Horn
 * @since 1.00
 * @version 2.00
 */
class CryptoEngineImplCaesar extends CryptoEngine {

    /**
     * Stores the shifting value for the symmetrical caesar encryption.
     * 
     * @since 1.00
     */
    private int shift = 1;

    /**
     * Creates a new encryption engine.
     * 
     * @param shift
     *            the shifting value for the encryption
     * 
     * @since 1.00
     */
    CryptoEngineImplCaesar(int shift) {
        this.shift = shift;
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
        String encrypt = "";
        int num;
        if (shift >= 0) {
            for (int i = 0; i < toEncrypt.length(); i++) {
                num = toEncrypt.charAt(i);
                num += shift + i;
                encrypt += (char) num;
            }
        } else {
            for (int i = 0; i < toEncrypt.length(); i++) {
                num = toEncrypt.charAt(i);
                num += shift - i;
                encrypt += (char) num;
            }
        }
        return encrypt;
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
        String decrypt = "";
        int num;
        int verschiebung = -this.shift;
        if (verschiebung >= 0) {
            for (int i = 0; i < toDecrypt.length(); i++) {
                num = toDecrypt.charAt(i);
                num += verschiebung + i;
                decrypt += (char) num;
            }
        } else {
            for (int i = 0; i < toDecrypt.length(); i++) {
                num = toDecrypt.charAt(i);
                num += verschiebung - i;
                decrypt += (char) num;
            }
        }
        return decrypt;
    }

}
