package pl.crypto.bigint;

import java.util.Arrays;
import java.util.Random;

public class BigInt extends Number implements Comparable<BigInt> {

    /*
    mask used to cast int into logn
    */
    private static final long MASK = (1L << 32) - 1;
    /*
    little endian digits represantiona
    */
    private int[] digits;
    /*
    size of digits aray
    */
    private int size;
    /*
    sign
    */
    private int signum;
    public static final BigInt ZERO = new BigInt(0);
    public static final BigInt ONE = new BigInt(1);
    public static final BigInt TWO = new BigInt(2);


    public BigInt(String value) {
        setUpDigits(value.toCharArray());
    }

    public BigInt(String value, int radix) {
        if (radix == 16) {
            digits = parseHexString(value);
            size = digits.length;
            signum = 1;
        }
    }

    public BigInt(BigInt big) {
        this.digits = big.digits;
        this.size = this.digits.length;
        this.signum = big.signum;
    }

    private BigInt(int[] digits, int signum) {
        this.digits = trimmLeadingZeros(digits);
        this.size = this.digits.length;
        this.signum = signum;
    }

    public BigInt(int value) {
        this(String.valueOf(value));
    }

    public BigInt(long value) {
        this(String.valueOf(value));
    }

    public BigInt(double value) {
        this(String.valueOf((long) value));
    }

    public BigInt(float value) {
        this(String.valueOf((long) value));
    }

    /**
     *  Returns proable prime number
     * @param bitLength
     * @return
     */
    public static BigInt getProbalePrime(int bitLength) {
        BigInt prime = BigInt.getRandom(bitLength);
        int it = 0;
        if (bitLength <512){
            it = 15;
        }else if (bitLength <1024){
            it = 10;
        }else{
            it = 3;
        }
        while (!BigInt.millerRabinTest(prime, it)) {
            prime = BigInt.getRandom(bitLength);
        }
        return prime;
    }

    /**
     * @param data
     */
    public BigInt(byte[] data) {
        signum = 1;
        int bitLength = data.length;
        digits = new int[bitLength / 4 + 1];
        for (int i = 0, j = 0; i < bitLength;) {
            digits[j] = (data[i++] & 0xFF);
            if (i >= bitLength) {
                break;
            }
            for (int k = 1; k < 4; k++) {
                digits[j] |= (data[i++] << (8 * k) & (0xff << 8 * k));
                if (i >= bitLength) {
                    break;
                }
            }
            j++;
        }
        digits = trimmLeadingZeros(digits);
        size = digits.length;
    }

    @Override
    public String toString() {
        if (isZero()) {
            return ("0");
        }
        if (digits.length == 0) {
            return "0";
        }
        int max = size * 10;
        final char[] buffer = new char[max];
        Arrays.fill(buffer, '0');
        final int[] copy = Arrays.copyOf(digits, size);
        while (true) {
            final int j = max;
            for (int tmp = udivDigits(1000000000); tmp > 0; tmp /= 10) {
                buffer[--max] += tmp % 10;
            }
            if (size == 1 && digits[0] == 0) {
                break;
            } else {
                max = j - 9;
            }
        }
        if (signum < 0) {
            buffer[--max] = '-';
        }
        System.arraycopy(copy, 0, digits, 0, size = copy.length);
        return new String(buffer, max, buffer.length - max);
    }

    /**
     * Generates random number
     * @param bitLength
     * @return
     */
    public static BigInt getRandom(int bitLength) {
        final int additionalInt = (bitLength % 32 == 0) ? 0 : 1;
        final int[] newDigits = new int[(bitLength / 32) + additionalInt];
        Random rand = new Random();
        for (int i = 0; i < newDigits.length - 1; i++) {
            newDigits[i] = rand.nextInt();
        }
        if (additionalInt == 1) {
            int minAdditionalInteger = (int) Math.pow(2, (bitLength % 32) - 1);
            int maxAdditionalInteger = 0xFFFFFFFF >>> (32 - (bitLength % 32));
            newDigits[newDigits.length - 1] = rand.nextInt(maxAdditionalInteger - minAdditionalInteger + 1) + minAdditionalInteger;
        } else {
            int tmp = 0x01;
            for (int i = 0; i < 31; i++) {
                tmp <<= 1;
                tmp |= rand.nextInt(2);
            }
            newDigits[newDigits.length - 1] = tmp;
        }
        return new BigInt(newDigits, 1);
    }

    public String toHexString() {
        final StringBuilder sb = new StringBuilder("");
        for (int i = digits.length - 1; i >= 0; i--) {
            StringBuilder sub = new StringBuilder(Integer.toHexString(digits[i]));
            if ((i < digits.length -1) && sub.length()< 8){
                for (int j = sub.length(); j < 8; j ++){
                    sub.insert(0, '0');
                }

            }
            sb.append(sub.toString());
        }
        int leadingZeros = 0;
        for (int i = 0; i < sb.length() - 1; i++){
            if (sb.charAt(i) == '0'){
                leadingZeros++;
            }
            else break;
        }
        sb.delete(0, leadingZeros);

        return sb.toString().toUpperCase();
    }

    /**
     * Checks if BigInt is zero
     * @return
     */
    public boolean isZero() {
        for (int i : digits) {
            if (i != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds this+other
     * @param other
     * @return
     */
    public BigInt add(final BigInt other) {
        int[] newDigits;
        int newSignum = 1;
        if (this.signum + other.signum > 0) {
            newDigits = addition(other.digits, this.digits);
            newSignum = 1;
        } else {
            return subtract(other);
        }
        return new BigInt(newDigits, newSignum);
    }

    /**
     * Substrats this-other
     * @param other
     * @return
     */
    public BigInt subtract(final BigInt other) {
        int[] newDigits;
        int newSignum;
        int compare = this.compareTo(other);
        if (compare == 0) {
            return new BigInt("0");
        } else if (compare > 0) {
            newDigits = subtraction(this.digits, other.digits);
            newSignum = 1;
        } else {
            newDigits = subtraction(other.digits, this.digits);
            newSignum = -1;
        }
        return new BigInt(trimmLeadingZeros(newDigits), newSignum);
    }

    /**
     * Multiplies this*other
     * @param other
     * @return
     */
    public BigInt multiply(final BigInt other) {
        if (this.isZero() || other.isZero()) {
            return ZERO;
        }
        int[] newDigits = multiplication(digits, size, 0, other.digits, other.size, 0);
        return new BigInt(newDigits, signum * other.signum);
    }

    public BigInt shift(int shift) {
        return new BigInt(shiftBaseTimes(digits, shift), signum);
    }

    /**
     * Exponentiation this**exponent
     * @param exponent
     * @return
     */
    public BigInt pow(int exponent) {
        if (exponent == 0) {
            return new BigInt("1");
        } else if (exponent < 0) {
            throw new ArithmeticException("negative exponent");
        } else {
            BigInt big = new BigInt(this.toString());
            for (int i = 1; i < exponent; i++) {
                big = big.multiply(this);

            }
            return big;
        }
    }

    /**
     * Greatest common divisor of big1 and big2
     * @param big1
     * @param big2
     * @return
     */
    public static BigInt gcd(BigInt big1, BigInt big2) {
        if (big2.compareTo(BigInt.ZERO) == 0) {
            return big1;
        } else {
            return BigInt.gcd(big2, big1.mod(big2));
        }
    }

    /**
     * Divides this/divider
     * @param divider
     * @return
     */
    public BigInt divide(BigInt divider) {
        if (divider.isZero()) {
            throw new ArithmeticException("dividing by zero");
        }
        if (divider.size == 1) {
            int[] newDigits = Arrays.copyOf(digits, size);
            udiv(divider.digits[0], newDigits);
            return new BigInt(trimmLeadingZeros(newDigits), this.signum * divider.signum);
        }
        if (this.compareTo(divider) == 0) {
            return ONE;
        }
        if (this.compareTo(divider) < 0) {
            return ZERO;
        }

        final int[] newDigits = new int[this.size - divider.size + 1];
        if (size == digits.length) {
            final int[] res = new int[size + 1];
            System.arraycopy(digits, 0, res, 0, size);
            digits = res;
        }
        int[] divident = Arrays.copyOf(digits, size + 1);
        int[] div = Arrays.copyOf(divider.digits, divider.size + 1);
        knuthDividing(divident, div, size, divider.size, newDigits);
        return new BigInt(trimmLeadingZeros(newDigits), 1);

    }

    public BigInt shiftLeft(int n) {
        if (n == 0) {
            return this;
        } else {
            return new BigInt(shiftLeft(digits, n), this.signum);
        }

    }

    /**
     * Creates bytes represation of BigInt
     * @return
     */
    public byte[] toByteArray() {
        final int byteLength = bitLength() / 8 + 1;
        byte[] byteArray = new byte[byteLength];

        for (int i = 0, j = 0; i < digits.length; i++) {
            int currentInt = digits[i];
            for (int k = 0; k < 4; k++) {
                if (j >= byteLength) {
                    break;
                }
                byteArray[j++] = (byte) currentInt;
                currentInt >>>= 8;
            }
        }
        if (byteArray[byteLength - 1] == 0x00) {
            byteArray = Arrays.copyOfRange(byteArray, 0, byteLength - 1);
        }
        return byteArray;
    }

    public BigInt shiftOneRight() {
        return shiftR();
    }

    /**
     * Abstract value of this
     * @return
     */
    public BigInt abs() {
        if (this.signum == 1) {
            return this;
        } else {
            return new BigInt(this.digits, 1);
        }
    }

    /**
     * Modulo this%m
     * @param m
     * @return
     */
    public BigInt mod(BigInt m) {
        if (m.isZero()) {
            throw new ArithmeticException();
        }
        final BigInt quotient = this.divide(m);
        final BigInt mul = m.multiply(quotient);
        final BigInt reminder = this.subtract(mul);
        return reminder;
    }

    /**
     * Calculates number of bits
     * @return
     */
    public int bitLength() {
        final int length = digits.length;
        if (length == 0) {
            return 0;
        }
        final int tmp1 = (length - 1) << 5;
        final int tmp2 = 32 - Integer.numberOfLeadingZeros(digits[length - 1]);
        return tmp1 + tmp2;
    }

    /**
     * Modular exponentiation this**exp mod m
     * @param exp
     * @param m
     * @return
     */
    public BigInt modPow(BigInt exp, BigInt m) {
        exp = new BigInt(exp.digits, 1);
        if (exp.isZero()) {
            return ONE;
        } else if (exp.signum > 0) {
            return modPositivePow(exp, m);
        } else {
            return null;
        }
    }

    /*
    translates []chars into []int and saves it in this.digits
    */
    private void setUpDigits(char[] chars) {
        signum = 1;
        if (chars[0] == '-') {
            signum = -1;
        }
        size = chars.length + (signum - 1 >> 1);
        int allocation;
        if (size < 10) {
            allocation = 1;
        } else {
            allocation = (int) (size * 3402L >>> 10) + 32 >>> 5;
        }
        if (digits == null || digits.length < allocation) {
            digits = new int[allocation];
        }

        int i = size % 9;
        if (i == 0) {
            i = 9;
        }
        i -= (signum - 1 >> 1);

        digits[0] = parseChars(chars, 0 - (signum - 1 >> 1), i);
        for (size = 1; i < chars.length;) {
            int parse = parseChars(chars, i, i += 9);
            multiAdd(1000000000, parse);
        }
    }
    /*
    simple addition
    */
    private int[] addition(final int[] n1, final int[] n2) {
        final int size1 = n1.length;
        final int size2 = n2.length;
        final int[] add = new int[size1 + 1];
        long reminder = 0;
        for (int i = 0; i < size1; i++) {
            int digit1 = n1[i];
            int digit2;
            if (i >= size2) {
                digit2 = 0;
            } else {
                digit2 = n2[i];
            }
            reminder = (digit1 & MASK) + (digit2 & MASK) + reminder;
            add[i] = (int) (reminder & MASK);
            reminder >>>= 32;
        }

        add[size1] = (int) (reminder & MASK);

        if (add[size1] == 0) {
            return Arrays.copyOfRange(add, 0, size1);
        }
        return add;
    }
    /*
    simple subtraction
    */
    private int[] subtraction(final int[] n1, final int[] n2) {
        final int size1 = n1.length;
        final int size2 = n2.length;

        final int[] sub = new int[size1];
        long reminder = 0;
        int i = 0;
        for (; i < size1; i++) {
            int digit1 = n1[i];
            int digit2;
            if (i >= size2) {
                digit2 = 0;
            } else {
                digit2 = n2[i];
            }
            reminder = (digit1 & MASK) - (digit2 & MASK) + reminder;
            sub[i] = (int) (reminder);
            reminder >>= 32;
        }
        return sub;
    }
    /*
    Recursive implementation of multiplication using Karatsuba Algorithm
    https://en.wikipedia.org/wiki/Karatsuba_algorithm
    */
    private int[] multiplication(int[] n1, int size1, int startIndex1, int[] n2, int size2, int startIndex2) {
        /*
        for small magnitute use simple multiplication
        */
        if (size1 <= 128 || size2 <= 128) {
            return smallMultiplication(n1, size1, startIndex1, n2, size2, startIndex2);
        } else {
            int split = size1 / 2;
            if (size2 < size1) {
                split = size2 / 2;
            }
            final int[] part1 = multiplication(n2, size2 - split, split, n1, size1 - split, split);//a c
            final int[] part2 = multiplication(n2, split, 0, n1, split, 0);//b d
            final int[] a_b = addArrays(n2, size2 - split, split, n2, split, 0);
            final int[] c_d = addArrays(n1, size1 - split, split, n1, split, 0);
            final int a_bSize = a_b.length;
            final int c_dSize = c_d.length;
            final int[] part3 = multiplication(a_b, a_bSize, 0, c_d, c_dSize, 0);

            return addArrays(addArrays(shiftBaseTimes(part1, 2 * split), shiftBaseTimes(subtraction(part3, addArrays(part1, part2)), split)), part2);
        }
    }
    /*
    Miller-Rabin test for primality
    */
    private static boolean millerRabinTest(BigInt number, int iterations) {
        if ((number.digits[0] & 0x01) == 0){
            return false;
        }
        final BigInt minusOne = number.subtract(ONE);
        final BigInt mm = new BigInt(TWO.pow(minusOne.bitLength() - 1));
        final BigInt m = minusOne.shiftLeft(mm.intValue());
        for (int k = 0; k < iterations; k++) {
            BigInt x = BigInt.getRandom(number.bitLength());
            while (x.compareTo(number) >= 0 || x.compareTo(ZERO) <= 0) {
                x = BigInt.getRandom(number.bitLength());
            }
            int j = 0;
            BigInt y = x.modPow(m, number);
            while (!((j == 0 && y.compareTo(ONE) == 0) || y.compareTo(minusOne) == 0)) {
                if (j > 0 && y.equals(ONE) || ++j == minusOne.bitLength())
                    return false;
                y = y.modPow(TWO, number);
            }
        }
        return true;
    }
    /*
    shifts []m n's times to the left
    */
    private int[] shiftLeft(int[] mag, int n) {
        final int ints = n >>> 5;
        final int bits1 = n & 0x1f;
        final int dLength = mag.length;
        int result[] = null;

        if (bits1 == 0) {
            result = new int[dLength + ints];
            System.arraycopy(mag, 0, result, 0, dLength);
        } else {
            int i = 0;
            int bits2 = 32 - bits1;
            int highBits = mag[0] >>> bits2;
            if (highBits != 0) {
                result = new int[dLength + ints + 1];
                result[i++] = highBits;
            } else {
                result = new int[dLength + ints];
            }
            int j = 0;
            while (j < dLength - 1) {
                result[i++] = mag[j++] << bits1 | mag[j] >>> bits2;
            }
            result[i] = mag[j] << bits1;
        }
        return result;
    }
    /*
    divides []n by div, returns reminder
    */
    private int udiv(final int div, int[] n) {
        final long d = div & MASK;
        long rem = 0;
        int len = n.length;
        for (int i = len - 1; i >= 0; i--) {
            rem <<= 32;
            rem = rem + (n[i] & MASK);
            n[i] = (int) (rem / d);
            rem = rem % d;
        }
        return (int) rem;
    }
    /*
    shifts this.digits by one bit to the right
    */
    private BigInt shiftR() {
        final int newMag[] = new int[size];
        newMag[0] = digits[0] >>> 1;
        for (int i = 1; i < size; i++) {
            int tmp = digits[i] << 31;
            int tmp2 = newMag[i - 1] | tmp;
            newMag[i - 1] = tmp2;
            newMag[i] = digits[i] >>> 1;
        }
        return new BigInt((newMag), signum);
    }

    /**
     * Checks if least significant bit is set (number is odd).
     * @return
     */
    public boolean leastSignificantBit() {
        if ((this.digits[0] & 0x01) == 1) {
            return true;
        }
        return false;
    }

    /*
    divides this.digits[] by div
    */
    private int udivDigits(final int div) {
        final long d = div & MASK;
        long rem = 0;
        for (int i = size - 1; i >= 0; i--) {
            rem <<= 32;
            rem = rem + (digits[i] & MASK);
            digits[i] = (int) (rem / d);
            rem = rem % d;
        }
        if (digits[size - 1] == 0 && size > 1) {
            --size;
        }

        return (int) rem;
    }

    /*
    implementation of modular exponentiation with positive exponent
    */
    private BigInt modPositivePow(BigInt exp, BigInt mod) {
        final BigInt x = this;
        BigInt s = ONE;
        BigInt z = x.mod(mod);
        int mask;
        for (int i = 0; i < exp.size; i++) {
            mask = 0x01;
            for (int j = 0; j < 32; j++) {
                if ((exp.digits[i] & mask) == mask) {
                    s = s.multiply(z).mod(mod);
                }
                z = z.multiply(z).mod(mod);
                mask <<= 1;
            }
        }
        s = new BigInt(s.digits, 1);
        return s;
    }

    /*
    adds two arrays from given index
    */
    private int[] addArrays(final int[] n1, int size1, int startIndex1, final int[] n2, int size2, int startIndex2) {
        if (n1.length >= n2.length) {
            return subArrayAddition(n1, size1, startIndex1, n2, size2);
        } else {
            return subArrayAddition(n2, size2, startIndex2, n1, size1);
        }
    }

    /*
    adds two whole arrays
    */
    private int[] addArrays(int[] n1, int[] n2) {
        if (n1.length >= n2.length) {
            return addition(n1, n2);
        } else {
            return addition(n2, n1);
        }
    }

    private int[] subArrayAddition(final int[] n1, final int size1, int startIndex1, int[] n2, int size2) {
        final int[] add = new int[size1 + 1];
        long reminder = 0;
        for (int i = startIndex1; i < size1; i++) {
            int digit1 = n1[i];
            int digit2;
            if (i >= size2) {
                digit2 = 0;
            } else {
                digit2 = n2[i];
            }
            reminder = (digit1 & MASK) + (digit2 & MASK) + reminder;
            add[i] = (int) (reminder & MASK);
            reminder >>>= 32;
        }

        add[size1] = (int) (reminder & MASK);
        if (add[size1] == 0) {
            return Arrays.copyOfRange(add, 0, size1);
        }
        return add;
    }

    /*
    long multplication
    */
    private int[] smallMultiplication(int[] n1, int size1, int startIndex1, int[] n2, int size2, int startIndex2) {
        final int[] result = new int[size1 + size2];
        long carry = 0;
        long tmp;
        long fArg = n1[0] & MASK;
        for (int i = startIndex1; i < size2; i++) {
            tmp = fArg * (n2[i] & MASK) + carry;
            result[i] = (int) tmp;
            carry = tmp >>> 32;
        }
        result[size2] = (int) carry;

        for (int i = 1; i < size1; i++) {
            fArg = n1[i] & MASK;
            carry = 0;
            for (int j = 0; j < size2; j++) {
                tmp = fArg * (n2[j] & MASK) + (result[i + j] & MASK) + carry;
                result[i + j] = (int) tmp;
                carry = tmp >>> 32;
            }
            result[size2 + i] = (int) carry;
        }
        return result;
    }

    /*
    removes leading zeros from []n if present
    */
    private int[] trimmLeadingZeros(final int[] n) {
        int zeros = 0;
        for (int i = n.length - 1; i >= 0; i--) {
            if (n[i] != 0) {
                break;
            }
            zeros++;
        }
        if (zeros == 0) {
            return n;
        } else if (zeros == n.length) {
            int ret[] = {0};
            return ret;
        } else {
            return Arrays.copyOfRange(n, 0, n.length - zeros);
        }
    }

    private int[] shiftBaseTimes(final int[] n, int shift) {
        if (shift == 0) {
            return n;
        }
        int len = n.length;
        if (len + shift <= 0) {
            return new int[1];
        }
        int[] result = new int[len + shift];
        if (shift > 0) {
            for (int i = 0; i < len; i++) {
                result[i + shift] = n[i];
            }
        } else {
            for (int i = 0; i < len + shift; i++) {
                result[i] = n[i - shift];
            }
        }
        return result;
    }

    private int parseChars(final char[] chars, int from, final int to) {
        int result = chars[from] - '0';
        while (++from < to) {
            result = result * 10 + chars[from] - '0';
        }
        return result;
    }

    private void multiAdd(final int mul, final int add) {
        long reminder = 0;
        for (int i = 0; i < size; i++) {
            reminder = mul * (digits[i] & MASK) + reminder;
            digits[i] = (int) reminder;
            reminder >>>= 32;
        }
        if (reminder != 0) {
            digits[size++] = (int) reminder;
        }
        reminder = (digits[0] & MASK) + add;
        digits[0] = (int) reminder;

        if ((reminder >>> 32) != 0) {
            int i = 1;
            while (i < size && ++digits[i] == 0) {
                ++i;
            }
            if (i == size) {
                digits[size++] = 1;
            }
        }

    }

    private int[] parseHexString(String hexString) {
        final int len = hexString.length();
        final int tail = len%8;
        int[] dig = new int[len / 8 + ((tail ==0) ? 0 : 1)];
        for (int i = len, j = 0; i >= 0; i-= 8){
            if (i <= tail && tail > 0 ){
                dig[dig.length- 1] = Integer.parseUnsignedInt(hexString.substring(0 ,tail), 16);
                break;
            }
            if (i < 8) break;
            dig[j++] = Integer.parseUnsignedInt(hexString.substring(i - 8, i), 16);
        }
        return dig;
    }

    /*
    Hacker'sDelight's implementation of Knuth's Algorithm D
    */
    private void knuthDividing(final int[] u, final int[] v, final int m, final int n, final int[] quotient) {
        final long base = 1L << 32;
        long qhat;
        long rhat;
        long product;

        int s, i, j;
        long t, k;
        s = Integer.numberOfLeadingZeros(v[n - 1]);
        if (s > 0) {
            for (i = n - 1; i > 0; i--) {
                v[i] = (v[i] << s) | (v[i - 1] >>> 32 - s);
            }
            v[0] = v[0] << s;

            u[m] = u[m - 1] >>> 32 - s;
            for (i = m - 1; i > 0; i--) {
                u[i] = (u[i] << s) | (u[i - 1] >>> 32 - s);
            }
            u[0] = u[0] << s;
        }

        final long dh = v[n - 1] & MASK, dl = v[n - 2] & MASK, hbit = Long.MIN_VALUE;

        for (j = m - n; j >= 0; j--) {
            k = u[j + n] * base + (u[j + n - 1] & MASK);
            qhat = (k >>> 1) / dh << 1;
            t = k - qhat * dh;
            if (t + hbit > dh + hbit) {
                qhat++;
            }
            rhat = k - qhat * dh;

            while (qhat + hbit >= base + hbit || qhat * dl + hbit > base * rhat + (u[j + n - 2] & MASK) + hbit) {
                qhat = qhat - 1;
                rhat = rhat + dh;
                if (rhat + hbit >= base + hbit) {
                    break;
                }
            }

            k = 0;
            for (i = 0; i < n; i++) {
                product = qhat * (v[i] & MASK);
                t = (u[i + j] & MASK) - k - (product & MASK);
                u[i + j] = (int) t;
                k = (product >>> 32) - (t >> 32);
            }
            t = (u[j + n] & MASK) - k;
            u[j + n] = (int) t;

            quotient[j] = (int) qhat;
            if (t < 0) {
                quotient[j] = quotient[j] - 1;
                k = 0;
                for (i = 0; i < n; i++) {
                    t = (u[i + j] & MASK) + (v[i] & MASK) + k;
                    u[i + j] = (int) t;
                    k = t >>> 32;
                }
                u[j + n] += (int) k;
            }
        }

        if (s > 0) {
            for (i = 0; i < n - 1; i++) {
                v[i] = v[i] >>> s | v[i + 1] << 32 - s;
            }
            v[n - 1] >>>= s;

            for (i = 0; i < m; i++) {
                u[i] = u[i] >>> s | u[i + 1] << 32 - s;
            }
            u[m] >>>= s;
        }
    }

    @Override
    public int compareTo(BigInt other) {
        if (this.signum * other.signum < 0) {
            if (this.signum < 0) {
                return -1;
            } else {
                return 1;
            }
        }
        if (size > other.size) {
            return 1;
        }
        if (size < other.size) {
            return -1;
        }
        for (int i = size - 1; i >= 0; i--) {
            if ((digits[i] & MASK) != (other.digits[i] & MASK)) {
                if ((digits[i] & MASK) > (other.digits[i] & MASK)) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
        return 0;
    }

    @Override
    public int intValue() {
        return signum * (digits[0] & 0xFFFFFFFF);
    }

    @Override
    public long longValue() {
        return signum * ((digits[1] & 0x7FFFFFFFL) << 32 | (digits[0] & MASK));
    }

    @Override
    public float floatValue() {
        return (float) intValue();
    }

    @Override
    public double doubleValue() {
        return (double) longValue();
    }

    public int isNegative() {
        return signum;
    }

    public int getSize() {
        return size;
    }

}
