package pl.crypto.elgamal;

import pl.crypto.bigint.BigInt;
import pl.crypto.keys.Keys;

import java.util.*;

public class ElGamalAlgorithm {

    int blockLength;

    public ElGamalAlgorithm() {

    }

    public byte[] encrypt(byte[] in, Keys key) {
        blockLength = key.getP().bitLength()/8;
        List<BigInt> message = chopMessage(in);
        List<BigInt> cryptogram = new LinkedList<>();

        BigInt c1;
        BigInt c2;
        for (int j = 0; j < message.size(); j++) {
            BigInt r = BigInt.getRandom(128);
            //c1 = g^r mod p
            c1 = key.getG().modPow(r, key.getP());
            cryptogram.add(c1);
            //c2 = h^r mod p * m
            c2 = key.getH().modPow(r, key.getP()).multiply(message.get(j));
            //c2 = c2 mod p
            c2 = c2.mod(key.getP());
            cryptogram.add(c2);
        }

        /*
        tranlates cryptogram into list of bytes
         */
        List<Byte> crypt = new LinkedList<>();
        for (BigInt word : cryptogram) {
            int fill = 0;
            int len = word.toByteArray().length;
            for (int i = 0; i < len; i++) {
                crypt.add(word.toByteArray()[i]);
                fill++;
            }
            for (int i = fill; i < blockLength; i++) {
                crypt.add((byte) 0x00);
            }
        }
        /*
        merges list of bytes into return array
         */
        byte[] data = new byte[crypt.size()];
        for (int i = 0; i < crypt.size(); i++) {
            data[i] = crypt.get(i);
        }
        cryptogram.clear();

        return data;
    }

    public byte[] decrypt(byte[] crypt, Keys key) {
        blockLength = key.getP().bitLength()/8;

        List<BigInt> cryptogram = chopCryptogram(crypt);
        List<byte[]> decrypted = new ArrayList<>();
        BigInt one = new BigInt(1);
        for (int i = 0; i < cryptogram.size();) {
            BigInt c1 = cryptogram.get(i);
            BigInt c2 = cryptogram.get(i + 1);
            // m1 = c1^(p - 1 - a) mod p
            BigInt m1 = c1.modPow(key.getP().subtract(one).subtract(key.getA()), key.getP());
            //m2 = m1 * c2 mod p
            BigInt m2 = (m1.multiply(c2)).mod(key.getP());
            decrypted.add(m2.toByteArray());
            i += 2;
        }

        return mergeMessage(decrypted);
    }

    /*
    splits message into 64-byte long blocks
     */
    private List<BigInt> chopMessage(byte[] rawData) {
        List<BigInt> message = new LinkedList<>();
        byte[] baseData = Base64.getEncoder().encode(rawData);
        int len = baseData.length;
        for (int i = 0; i < len; i += blockLength) {
            if (i + blockLength > len) {
                BigInt tmp = new BigInt(Arrays.copyOfRange(baseData, i, len));
                message.add(tmp);
                break;
            }
            BigInt tmp = new BigInt(Arrays.copyOfRange(baseData, i, i + blockLength));
            message.add(tmp);
        }
        return message;
    }

    /*
    splits cipher into 64-byte long blocks
     */
    private List<BigInt> chopCryptogram(byte[] cryptoData) {
        List<BigInt> cryptogram = new LinkedList<>();
        int cryptLength = cryptoData.length;
        for (int i = 0; i < cryptLength; i += blockLength) {
            cryptogram.add(new BigInt(Arrays.copyOfRange(cryptoData, i, i + blockLength)));
        }
        return cryptogram;
    }

    /*
    merges decrypted blocks into single message
     */
    private byte[] mergeMessage(List<byte[]> message) {
        int it = 0;
        for (byte[] b : message) {
            it += b.length;
        }
        byte[] decrypted = new byte[it];
        it = 0;
        for (int i = 0; i < message.size(); i++) {
            for (int j = 0; j < message.get(i).length; j++) {
                decrypted[it++] = message.get(i)[j];
            }
        }
        try {
            return Base64.getDecoder().decode(decrypted);
        } catch (Exception ex) {
            return decrypted;
        }
    }

}

