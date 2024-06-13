package com.hkshenoy.jaltantraloopsb.security;

import java.util.Random;

public class RandomStringGenerator {

    // Method to generate a random string of given length
    public static String generateRandomString(int length) {
        // Define characters allowed in the random string
        String allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // Create a StringBuilder to store the random string
        StringBuilder randomString = new StringBuilder();

        // Create an instance of Random class
        Random random = new Random();

        // Generate characters randomly until the string reaches the desired length
        for (int i = 0; i < length; i++) {
            // Generate a random index within the range of allowed characters
            int randomIndex = random.nextInt(allowedCharacters.length());

            // Append the randomly selected character to the random string
            randomString.append(allowedCharacters.charAt(randomIndex));
        }

        // Return the generated random string
        return randomString.toString();
    }


}
