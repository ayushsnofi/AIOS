
public class JwtService{
    @Value('${app.security.jwt.sceret:default-ultra-secure-secret}')
    private String sceretKey;

    @Value('${app.security.jwt.expiration:86400000}')
    private long jwtExpirationMs;

    private Key getSigningKey(){
        byte[] keyBytes = this.jwtSceret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserName(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims,T> claimsResolver){
        final Claims claims= extractAllClamins(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token,String userEmail){
        final String userName=extractUserName(token);
        return (userName.equals(userEmail))&&!isTokenExpired(token);
    }

    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token){
        return extractClaim(token,Claims::getExpiration);
    }

    private Claims extractAllClaims(String token){
        return Jwts.parserBuilder().setSigningKey(getSigningKey())
        .build().parseClaimsJws(token).getBody();
    }
}