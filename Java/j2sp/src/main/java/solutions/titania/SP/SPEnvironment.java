package solutions.titania.SP;

public interface SPEnvironment {

    String getUrl();

    void expireCookies();

    /**
     * 
     * @return cookie string of an authenticated session
     */
    String getCookieString();

}