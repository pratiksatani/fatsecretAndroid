package com.android.fatsecretandroid;


import android.util.Base64;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class helps in building requests for sending them to the fatsecret rest api
 *
 * @author Pratik Satani
 * @version 1.0
 */


public class RequestBuilder {
    /** A value FatSecret API issues to you which helps this API identify you
     * you will get this value from fatsecret account */
    final private String APP_KEY;

    /** A secret FatSecret API issues to you which helps this API establish that it really is you
     * you will get this value from fatsecret account */
    final private String APP_SECRET;

    /**
     * Request URL
     * The URL to make API calls is http://platform.fatsecret.com/rest/server.api
     */
    final private String APP_URL = "http://platform.fatsecret.com/rest/server.api";

    /**
     * The signature method allowed by FatSecret API
     * They only support "HMAC-SHA1"
     */
    final private String Oauth_SIGNATURE_METHOD = "HmacSHA1";

    /**
     * The HTTP Method supported by FatSecret API
     * This API only supports GET method
     */
    final private String HTTP_METHOD = "GET";


    /**
     * Constructor to set values for APP_KEY and APP_SECRET
     *
     * @param APP_KEY		a value FatSecret API issues to you which helps this API identify you
     * @param APP_SECRET	a secret FatSecret API issues to you which helps this API establish that it really is you
     */
    public RequestBuilder(String APP_KEY, String APP_SECRET) {
        this.APP_KEY = APP_KEY;
        this.APP_SECRET = APP_SECRET;
    }





    /**
     * Returns randomly generated nonce value for calling the request.
     *
     * @return				the randomly generated value for nonce.
     */



    public String nonce() {
        Random r = new Random();
        StringBuffer n = new StringBuffer();
        for (int i = 0; i < r.nextInt(8) + 2; i++) {
            n.append(r.nextInt(26) + 'a');
        }
        return n.toString();
    }

    /**
     * Returns all the oauth parameters and other parameters.
     *
     * @return 				an array of parameter values as "key=value" pair
     */
    public String[] generateOauthParams() {
        String[] a = {
                "oauth_consumer_key=" + APP_KEY,
                "oauth_signature_method=HMAC-SHA1",
                "oauth_timestamp=" + new Long(System.currentTimeMillis() / 1000).toString(),
                "oauth_nonce=" + nonce(),
                "oauth_version=1.0",
                "format=json"
        };
        return a;
    }

    /**
     * Returns the string generated using params and separator
     *
     * @param params		an array of parameter values as "key=value" pair
     * @param separator		a separator for joining
     *
     * @return				the string by appending separator after each parameter from params except the last.
     */
    public String join(String[] params, String separator) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                b.append(separator);
            }
            b.append(params[i]);
        }
        return b.toString();
    }

    /**
     * Returns string generated using params and "&amp;" for signature base and normalized parameters
     *
     * @param params		an array of parameter values as "key=value" pair
     * @return 				the string by appending separator after each parameter from params except the last.
     */
    public String paramify(String[] params) {
        String[] p = Arrays.copyOf(params, params.length);
        Arrays.sort(p);
        return join(p, "&");
    }

    /**
     * Returns the percent-encoded string for the given url
     *
     * @param url			URL which is to be encoded using percent-encoding
     * @return 				the encoded url
     */
    public String encode(String url) {
        if (url == null)
            return "";

        try {
            return URLEncoder.encode(url, "utf-8")
                    .replace("+", "%20")
                    .replace("!", "%21")
                    .replace("*", "%2A")
                    .replace("\\", "%27")
                    .replace("(", "%28")
                    .replace(")", "%29");
        }
        catch (UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }

    /**
     * Returns the signature generated using signature base as text and consumer secret as key
     *
     * @param method		http method
     * @param uri			request URL - http://platform.fatsecret.com/rest/server.api (Always remains the same)
     * @param params		an array of parameter values as "key=value" pair
     * @return				oauth_signature which will be added to request for calling fatsecret api
     * @throws UnsupportedEncodingException if encoding is unsupported
     */
    public String sign(String method, String uri, String[] params) throws UnsupportedEncodingException {
        String encodedURI = encode(uri);
        String encodedParams = encode(paramify(params));

        String[] p = {method, encodedURI, encodedParams};

        String text = join(p, "&");
        String key = APP_SECRET + "&";
        SecretKey sk = new SecretKeySpec(key.getBytes(), Oauth_SIGNATURE_METHOD);
        String sign = "";
        try {
            Mac m = Mac.getInstance(Oauth_SIGNATURE_METHOD);
            m.init(sk);
            sign = encode(new String(Base64.encode(m.doFinal(text.getBytes()), Base64.DEFAULT)).trim());
        } catch(java.security.NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException: " + e.getMessage());
        } catch(java.security.InvalidKeyException e) {
            System.out.println("InvalidKeyException: " + e.getMessage());
        }
        return sign;
    }

    /**
     * Returns the rest url which will be sent to fatsecret platform server for searching food items based on search terms and page number
     *
     * @param query			search terms for querying food items
     * @param pageNumber	page Number to search the food items
     * @return				rest url which will be sent to fatsecret platform server for searching food items
     * @throws Exception	if sign throws exception
     */
    public String buildFoodsSearchUrl(String query, int pageNumber) throws Exception {
        List<String> params = new ArrayList<String>(Arrays.asList(generateOauthParams()));
        String[] template = new String[1];
        params.add("method=foods.search");
        params.add("max_results=50");
        params.add("page_number=" + pageNumber);
        params.add("search_expression=" + encode(query));
        params.add("oauth_signature=" + sign(HTTP_METHOD, APP_URL, params.toArray(template)));

        return APP_URL + "?" + paramify(params.toArray(template));
    }

    /**
     * Returns the rest url which will be sent to fatsecret platform server for searching unique food item
     *
     * @param id			the unique food identifier
     * @return				rest url which will be sent to fatsecret platform server for searching unique food item
     * @throws Exception	if sign throws exception
     */
    public String buildFoodGetUrl(Long id) throws Exception {
        List<String> params = new ArrayList<String>(Arrays.asList(generateOauthParams()));
        String[] template = new String[1];
        params.add("method=food.get");
        params.add("food_id=" + id);
        params.add("oauth_signature=" + sign(HTTP_METHOD, APP_URL, params.toArray(template)));

        return APP_URL + "?" + paramify(params.toArray(template));
    }


    /**
     * Returns the rest url which will be sent to fatsecret platform server for searching unique food item
     *
     * @param userID			The user_id specified in profile.create.
     * @return				rest url which will be sent to fatsecret platform server for getting oauth_token and oauth_secret of user
     * @throws Exception	if sign throws exception
     */
    public String buildGetUserAuthUrl(String userID) throws Exception {
        List<String> params = new ArrayList<String>(Arrays.asList(generateOauthParams()));
        String[] template = new String[1];
        params.add("method=profile.get_auth");
        params.add("user_id=" + userID);
        params.add("oauth_signature=" + sign(HTTP_METHOD, APP_URL, params.toArray(template)));

        return APP_URL + "?" + paramify(params.toArray(template));
    }

    /**
     * Returns the rest url which will be sent to fatsecret platform server for searching unique food item
     *
     * @param OauthToken			The key of the profile to use
     * @return				rest url which will be sent to fatsecret platform server for getting the food elements returned are those that were recently eaten by the user.
     * @throws Exception	if sign throws exception
     */
    public String buildGetRecentlyEatenFoodUrl(String OauthToken) throws Exception {
        List<String> params = new ArrayList<String>(Arrays.asList(generateOauthParams()));
        String[] template = new String[1];
        params.add("method=foods.get_recently_eaten");
        params.add("oauth_token=" + OauthToken);
        params.add("oauth_signature=" + sign(HTTP_METHOD, APP_URL, params.toArray(template)));

        return APP_URL + "?" + paramify(params.toArray(template));
    }





    public String buildFoodSearchbarcodeGetUrl(String Barcode) throws Exception {
        List<String> params = new ArrayList<String>(Arrays.asList(generateOauthParams()));
        String[] template = new String[1];
        params.add("method=food.find_id_for_barcode");
        params.add("barcode=" + Barcode);
        params.add("oauth_signature=" + sign(HTTP_METHOD, APP_URL, params.toArray(template)));

        return APP_URL + "?" + paramify(params.toArray(template));
    }

    /**
     * Returns the rest url which will be sent to fatsecret platform server for searching recipes
     *
     * @param query			search terms for querying recipes
     * @param pageNumber	page Number to search the recipes
     * @return				rest url which will be sent to fatsecret platform server for searching recipes
     * @throws Exception	if sign throws exception
     */
    public String buildRecipesSearchUrl(String query, int pageNumber) throws Exception {
        List<String> params = new ArrayList<String>(Arrays.asList(generateOauthParams()));
        String[] template = new String[1];
        params.add("method=recipes.search");
        params.add("max_results=50");
        params.add("page_number=" + pageNumber);
        params.add("search_expression=" + encode(query));
        params.add("oauth_signature=" + sign(HTTP_METHOD, APP_URL, params.toArray(template)));

        return APP_URL + "?" + paramify(params.toArray(template));
    }

    /**
     * Returns the rest url which will be sent to fatsecret platform server for searching unique recipe
     *
     * @param id			the unique recipe identifier
     * @return				rest url which will be sent to fatsecret platform server for searching unique recipe
     * @throws Exception	if sign throws exception
     */
    public String buildRecipeGetUrl(Long id) throws Exception {
        List<String> params = new ArrayList<String>(Arrays.asList(generateOauthParams()));
        String[] template = new String[1];
        params.add("method=recipe.get");
        params.add("recipe_id=" + id);
        params.add("oauth_signature=" + sign(HTTP_METHOD, APP_URL, params.toArray(template)));

        return APP_URL + "?" + paramify(params.toArray(template));
    }

    /*
    * Returns the rest url which will be sent to fatsecret platform server for creating new profile
    * */
    public String crateProfile(String userId)
    {
       // this.createProfileCallback=callback;
        List<String> params = new ArrayList<String>(Arrays.asList(generateOauthParams()));
        String[] template = new String[1];
        params.add("method=profile.create");
        params.add("user_id="+userId);
        try {
            params.add("oauth_signature=" + sign(HTTP_METHOD, APP_URL, params.toArray(template)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return APP_URL + "?" + paramify(params.toArray(template));
    }

}
