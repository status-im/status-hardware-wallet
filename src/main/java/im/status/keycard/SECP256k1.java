package im.status.keycard;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.ECKey;
import javacard.security.ECPrivateKey;
import javacard.security.KeyAgreement;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.RSAPublicKey;
import javacardx.crypto.Cipher;

/**
 * Utility methods to work with the SECP256k1 curve. This class is not meant to be instantiated, but its init method
 * must be called during applet installation.
 */
public class SECP256k1 {
  static final byte SECP256K1_FP[] = {
      (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
      (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
      (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
      (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE,(byte)0xFF,(byte)0xFF,(byte)0xFC,(byte)0x2F
  };
  static final byte SECP256K1_A[] = {
      (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
      (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
      (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
      (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
  };
  static final byte SECP256K1_B[] = {
      (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
      (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
      (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
      (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x07
  };
  static final byte SECP256K1_G[] = {
      (byte)0x04,
      (byte)0x79,(byte)0xBE,(byte)0x66,(byte)0x7E,(byte)0xF9,(byte)0xDC,(byte)0xBB,(byte)0xAC,
      (byte)0x55,(byte)0xA0,(byte)0x62,(byte)0x95,(byte)0xCE,(byte)0x87,(byte)0x0B,(byte)0x07,
      (byte)0x02,(byte)0x9B,(byte)0xFC,(byte)0xDB,(byte)0x2D,(byte)0xCE,(byte)0x28,(byte)0xD9,
      (byte)0x59,(byte)0xF2,(byte)0x81,(byte)0x5B,(byte)0x16,(byte)0xF8,(byte)0x17,(byte)0x98,
      (byte)0x48,(byte)0x3A,(byte)0xDA,(byte)0x77,(byte)0x26,(byte)0xA3,(byte)0xC4,(byte)0x65,
      (byte)0x5D,(byte)0xA4,(byte)0xFB,(byte)0xFC,(byte)0x0E,(byte)0x11,(byte)0x08,(byte)0xA8,
      (byte)0xFD,(byte)0x17,(byte)0xB4,(byte)0x48,(byte)0xA6,(byte)0x85,(byte)0x54,(byte)0x19,
      (byte)0x9C,(byte)0x47,(byte)0xD0,(byte)0x8F,(byte)0xFB,(byte)0x10,(byte)0xD4,(byte)0xB8
  };
  static final byte SECP256K1_R[] = {
      (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
      (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFE,
      (byte)0xBA,(byte)0xAE,(byte)0xDC,(byte)0xE6,(byte)0xAF,(byte)0x48,(byte)0xA0,(byte)0x3B,
      (byte)0xBF,(byte)0xD2,(byte)0x5E,(byte)0x8C,(byte)0xD0,(byte)0x36,(byte)0x41,(byte)0x41
  };

  static final byte SECP256K1_K = (byte)0x01;

  static final short SECP256K1_KEY_SIZE = 256;
  static final short SECP256K1_BYTE_SIZE = (short) (SECP256K1_KEY_SIZE / 8);

  static final short SCHNORR_K_OUT_OFF = (short) 0;
  static final short SCHNORR_E_OUT_OFF = (short) (32 + SCHNORR_K_OUT_OFF);
  static final short SCHNORR_E_32_OFF = (short) (32 + SCHNORR_E_OUT_OFF);
  static final short SCHNORR_D_OUT_OFF = (short) (64 + SCHNORR_E_OUT_OFF);
  static final short SCHNORR_D_32_OFF = (short) (32 + SCHNORR_D_OUT_OFF);
  static final short SCHNORR_TMP1_OUT_OFF = (short) (64 + SCHNORR_D_OUT_OFF);
  static final short SCHNORR_TMP1_32_OUT_OFF = (short) (32 + SCHNORR_TMP1_OUT_OFF);
  static final short SCHNORR_TMP2_OUT_OFF = (short) (64 + SCHNORR_TMP1_OUT_OFF);

  static final short TMP_LEN = 288;

  private static final byte ALG_EC_SVDP_DH_PLAIN_XY = 6; // constant from JavaCard 3.0.5

  private KeyAgreement ecPointMultiplier;
  private Crypto crypto;
  ECPrivateKey tmpECPrivateKey;

  private KeyPair multPair;
  private RSAPublicKey pow2;
  private Cipher multCipher;

  static final byte[] CONST_TWO = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02};

  private byte[] tmp;

  /**
   * Allocates objects needed by this class. Must be invoked during the applet installation exactly 1 time.
   */
  SECP256k1(Crypto crypto) {
    this.crypto = crypto;

    this.tmp = JCSystem.makeTransientByteArray(TMP_LEN, JCSystem.CLEAR_ON_RESET);

    this.ecPointMultiplier = KeyAgreement.getInstance(ALG_EC_SVDP_DH_PLAIN_XY, false);
    this.tmpECPrivateKey = (ECPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PRIVATE, SECP256K1_KEY_SIZE, false);
    setCurveParameters(tmpECPrivateKey);

    multPair = new KeyPair(KeyPair.ALG_RSA_CRT, KeyBuilder.LENGTH_RSA_512);
    multPair.genKeyPair();
    pow2 = (RSAPublicKey) multPair.getPublic();
    pow2.setExponent(CONST_TWO, (short) 0, (short) CONST_TWO.length);

    multCipher = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
    multCipher.init(pow2, Cipher.MODE_ENCRYPT);
  }

  /**
   * Sets the SECP256k1 curve parameters to the given ECKey (public or private).
   *
   * @param key the key where the curve parameters must be set
   */
  void setCurveParameters(ECKey key) {
    key.setA(SECP256K1_A, (short) 0x00, (short) SECP256K1_A.length);
    key.setB(SECP256K1_B, (short) 0x00, (short) SECP256K1_B.length);
    key.setFieldFP(SECP256K1_FP, (short) 0x00, (short) SECP256K1_FP.length);
    key.setG(SECP256K1_G, (short) 0x00, (short) SECP256K1_G.length);
    key.setR(SECP256K1_R, (short) 0x00, (short) SECP256K1_R.length);
    key.setK(SECP256K1_K);
  }

  /**
   * Derives the public key from the given private key and outputs it in the pubOut buffer. This is done by multiplying
   * the private key by the G point of the curve.
   *
   * @param privateKey the private key
   * @param pubOut the output buffer for the public key
   * @param pubOff the offset in pubOut
   * @return the length of the public key
   */
  short derivePublicKey(ECPrivateKey privateKey, byte[] pubOut, short pubOff) {
    return multiplyPoint(privateKey, SECP256K1_G, (short) 0, (short) SECP256K1_G.length, pubOut, pubOff);
  }


  /**
   * Derives the public key from the given private key and outputs it in the pubOut buffer. This is done by multiplying
   * the private key by the G point of the curve.
   *
   * @param privateKey the private key
   * @param pubOut the output buffer for the public key
   * @param pubOff the offset in pubOut
   * @return the length of the public key
   */
  short derivePublicKey(byte[] privateKey, short privOff, byte[] pubOut, short pubOff) {
    tmpECPrivateKey.setS(privateKey, privOff, SECP256K1_BYTE_SIZE);
    return derivePublicKey(tmpECPrivateKey, pubOut, pubOff);
  }

  /**
   * Multiplies a scalar in the form of a private key by the given point. Internally uses a special version of EC-DH
   * supported since JavaCard 3.0.5 which outputs both X and Y in their uncompressed form.
   *
   * @param privateKey the scalar in a private key object
   * @param point the point to multiply
   * @param pointOff the offset of the point
   * @param pointLen the length of the point
   * @param out the output buffer
   * @param outOff the offset in the output buffer
   * @return the length of the data written in the out buffer
   */
  short multiplyPoint(ECPrivateKey privateKey, byte[] point, short pointOff, short pointLen, byte[] out, short outOff) {
    ecPointMultiplier.init(privateKey);
    return ecPointMultiplier.generateSecret(point, pointOff, pointLen, out, outOff);
  }

  short signSchnorr(ECPrivateKey privKey, byte[] pubKey, short pubOff, byte[] data, short dataOff, short dataLen, byte[] output, short outOff) {
    /*
    The algorithm Sign(sk, m) is defined as:
    Let d' = int(sk)
    Fail if d' = 0 or d' ≥ n
    Let P = d'⋅G
    Let d = d' if has_square_y(P), otherwise let d = n - d' .
    Let rand = hashBIPSchnorrDerive(bytes(d) || m).
    Let k' = int(rand) mod n.
    Fail if k' = 0.
    Let R = k'⋅G.
    Let k = k' if has_square_y(R), otherwise let k = n - k' .
    Let e = int(hashBIPSchnorr(bytes(R) || bytes(P) || m)) mod n.
    Return the signature bytes(R) || bytes((k + ed) mod n).
    */
    //TODO: evaluate if mod must be really applied to the output of the RNG and SHA256, since the hash is statistically very unlikley to be higher than R

    crypto.random.generateData(tmp, SCHNORR_K_OUT_OFF, SECP256K1_BYTE_SIZE);
    Util.arrayFillNonAtomic(tmp, SCHNORR_E_OUT_OFF, (short)(TMP_LEN - SCHNORR_E_OUT_OFF), (byte) 0x0);

    derivePublicKey(tmp, SCHNORR_K_OUT_OFF, output, outOff);
    crypto.sha256.update(output, outOff, Crypto.KEY_PUB_SIZE);
    crypto.sha256.update(pubKey, pubOff, Crypto.KEY_PUB_SIZE);
    crypto.sha256.doFinal(data, dataOff, dataLen, tmp, SCHNORR_E_32_OFF);
    privKey.getS(tmp, SCHNORR_D_32_OFF);

    tmp[(short)(SCHNORR_TMP1_32_OUT_OFF - 1)] = (byte) crypto.add256(tmp, SCHNORR_E_32_OFF, tmp, SCHNORR_D_32_OFF, tmp, SCHNORR_TMP1_32_OUT_OFF);
    multCipher.doFinal(tmp, SCHNORR_TMP1_OUT_OFF, (short) 64, tmp, SCHNORR_TMP1_OUT_OFF);
    multCipher.doFinal(tmp, SCHNORR_D_OUT_OFF, (short) 64, tmp, SCHNORR_TMP2_OUT_OFF);
    crypto.sub512(tmp, SCHNORR_TMP1_OUT_OFF, tmp, SCHNORR_TMP2_OUT_OFF, tmp, SCHNORR_TMP1_OUT_OFF);
    multCipher.doFinal(tmp, SCHNORR_E_OUT_OFF, (short) 64, tmp, SCHNORR_TMP2_OUT_OFF);
    crypto.sub512(tmp, SCHNORR_TMP1_OUT_OFF, tmp, SCHNORR_TMP2_OUT_OFF, tmp, SCHNORR_TMP1_OUT_OFF);

    short res, res2;

    for (short i = (short) 63; i >= 0; i--) {
      res = (short) ((short) (tmp[(short)(SCHNORR_TMP1_OUT_OFF + i)] & 0xff) >> 1);
      res2 = (short) ((short) (tmp[(short)(SCHNORR_TMP1_OUT_OFF + i - 1)] & 0xff) << 7);
      tmp[(short)(SCHNORR_TMP1_OUT_OFF + i)] = (byte) (short) (res | res2);
    }

    tmp[SCHNORR_TMP1_OUT_OFF] &= (byte) 0x7f;

    add256to512(tmp, SCHNORR_TMP1_OUT_OFF, tmp, SCHNORR_K_OUT_OFF, output, (short) (outOff + Crypto.KEY_PUB_SIZE));

    return (short) (Crypto.KEY_PUB_SIZE  + 64);
  }

  short add256to512(byte[] a, short aOff,  byte[] b, short bOff, byte[] out, short outOff) {
    short outI = 0;

    for (short i = 63 ; i >= 32 ; i--) {
      outI = (short) ((short)(a[(short)(aOff + i)] & 0xFF) + (short)(b[(short)(bOff + 32 + i)] & 0xFF) + outI);
      out[(short)(outOff + i)] = (byte)outI;
      outI = (short)(outI >> 8);
    }

    for (short i = 31 ; i >= 0 ; i--) {
      outI = (short) ((short)(a[(short)(aOff + i)] & 0xFF) + outI);
      out[(short)(outOff + i)] = (byte)outI;
      outI = (short)(outI >> 8);
    }
    return outI;
  }
}
