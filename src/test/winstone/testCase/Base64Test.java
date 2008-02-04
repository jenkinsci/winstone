package winstone.testCase;

import junit.framework.TestCase;
import winstone.auth.BasicAuthenticationHandler;

public class Base64Test extends TestCase {
    public Base64Test(String name) {
        super(name);
    }

    // The letters a-y encoded in base 64
    private static String ENCODED_PLUS_ONE = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eQ==";
    private static String ENCODED_PLUS_TWO = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=";    
    
    public void testDecode() throws Exception {
        String decoded = decodeBase64(ENCODED_PLUS_TWO);
        String expected = "abcdefghijklmnopqrstuvwxyz";
        assertEquals("Straight decode failed", expected, decoded);
        
        decoded = decodeBase64(ENCODED_PLUS_ONE);
        expected = "abcdefghijklmnopqrstuvwxy";
        assertEquals("Decode failed", expected, decoded);
    }
    
    public static void testVersusPostgres() throws Exception {
        String decoded = decodeBase64("MTIzNDU2Nzg5MA==");
        assertEquals("Straight encode failed", "1234567890", decoded);
    }

    /**
     * Expects the classic base64 "abcdefgh=" syntax (equals padded)
     * and decodes it to original form
     */
    public static String decodeBase64(String input) {
        char[] inBytes = input.toCharArray();
        byte[] outBytes = new byte[(int) (inBytes.length * 0.75f)]; // always mod 4 = 0
        int length = BasicAuthenticationHandler.decodeBase64(inBytes, outBytes, 0, inBytes.length, 0);
        return new String(outBytes, 0, length);
    }

    public static String hexEncode(byte input[]) {

        StringBuffer out = new StringBuffer();

        for (int i = 0; i < input.length; i++)
            out.append(Integer.toString((input[i] & 0xf0) >> 4, 16))
               .append(Integer.toString(input[i] & 0x0f, 16));

        return out.toString();
    }
    
    public static byte[] hexDecode(String input) {

        if (input == null) {
            return null;
        } else if (input.length() % 2 != 0) {
            throw new RuntimeException("Invalid hex for decoding: " + input);
        } else {
            byte output[] = new byte[input.length() / 2];

            for (int i = 0; i < output.length; i++) {
                int twoByte = Integer.parseInt(input.substring(i * 2, i * 2 + 2), 16);
                output[i] = (byte) (twoByte& 0xff);
            }
            return output;
        }
    }
}
