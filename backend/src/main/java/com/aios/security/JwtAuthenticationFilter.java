
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRquest request,HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authHeader= request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if(authHeader==null|| !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        jwt=authHeader.subString(7);
        try{
            userEmail=jwtService.extractUserName(jwt);

            if(userEmail!=null && SecurityContextHolder.getContext().getAuthentication()==null){
                if(jwtService.isTokenValid(jwt,userEmail)){
                    UsernamePasswordAuthenticationToken authToken=new UsernamePasswordAuthenticationToken(userEmail,null,Collections.emptyList());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }catch(Exception e){
            logger.error("Error in JwtAuthenticationFilter: {}",e.getMessage());
        }
        filterChain.doFilter(request,response);
        
    }
}