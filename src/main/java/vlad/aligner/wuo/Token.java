package vlad.aligner.wuo;

public class Token {
    String token;
    boolean delimiter;

    public Token(String token, boolean delimiter){
        this.token = token;
        this.delimiter = delimiter;
    }

    public String getToken(){
        return token;
    }

    public boolean isDelimiter() {
        return delimiter;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setDelimiter(boolean delimiter) {
        this.delimiter = delimiter;
    }

    public String toString() {
        return "\""+token+"\", " + delimiter;
    }

}
