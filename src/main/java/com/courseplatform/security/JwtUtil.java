package com.courseplatform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.expiration-ms}")
	private Long expirationMs;

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject); // extract the "sub" claim which is the username
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token); // extract all claims from the token
		return claimsResolver.apply(claims); // apply the resolver function to get specific claim
	}

	public String generateToken( UserDetails userDetails ) {
		return generateToken(new HashMap<>(), userDetails); // generate token with no extra claims
	}

	public String generateToken( Map<String, Object> extraClaims, UserDetails userDetails) {
		return Jwts.builder()
			.claims(extraClaims) // set extra claims
			.subject(userDetails.getUsername()) // set subject as username
			.issuedAt(new Date(System.currentTimeMillis())) // set issued at current time
			.expiration(new Date(System.currentTimeMillis() + expirationMs)) // set expiration time
			.signWith(getSigningKey()) // sign the token with secret key
			.compact(); // build the token
	}

	public boolean isTokenValid( String token, UserDetails userDetails ) {
		final String username = extractUsername(token); // extract username from token
		// check if username matches and if token is expired or not
		return ( username.equals(userDetails.getUsername()) && !isTokenExpired(token) );
	}

	private boolean isTokenExpired( String token ) {
		try { // try to extract expiration date from token
			return extractExpiration(token).before(new Date()); // check if expiration date is before current date
		} catch ( Exception e ) { // if token is (invalid, tampered, malformed, broken etc) consider it expired and return true
			return true;
		}
	}

	private Date extractExpiration( String token ) {
		return extractClaim(token, Claims::getExpiration); // extract the "exp" claim which is the expiration date
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser()
			.verifyWith(getSigningKey()) // set the signing key for verification
			.build() // build the parser
			.parseSignedClaims(token) // parse the token
			.getPayload(); // get the claims payload
	}

	private SecretKey getSigningKey () {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey); // decode the base64 encoded secret key
		return Keys.hmacShaKeyFor(keyBytes); // create HMAC SHA key from the byte array
	}
}
