package pl.crypto.keys;

import pl.crypto.bigint.BigInt;

public class Keys {

    private BigInt p;
    private BigInt a;
    private BigInt g;
    private BigInt h;

    public Keys() {
    }

    public void generateKeys(int bitLength) {
        p = generatePrimeNumber(bitLength);
        a = generatePrivateKey(p);
        g = BigInt.getRandom(300);
        h = g.modPow(a, p);
    }

    public BigInt getP() {
        return p;
    }

    public BigInt getA() {
        return a;
    }

    public BigInt getG() {
        return g;
    }

    public BigInt getH() {
        return h;
    }

    public BigInt generatePrimeNumber(int bits) {
        return BigInt.getProbalePrime(bits);
    }

    private BigInt generatePrivateKey(BigInt p) {
        BigInt pMinusOne = p.subtract(BigInt.ONE);
        BigInt a = BigInt.getRandom(p.bitLength());
        while (a.compareTo(pMinusOne) > 0
                || a.compareTo(BigInt.ZERO) < 0) {
            a = BigInt.getRandom(p.bitLength());
        }
        return a;
    }

}
