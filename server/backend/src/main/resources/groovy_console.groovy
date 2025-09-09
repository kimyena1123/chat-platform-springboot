import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

def encoder = new BCryptPasswordEncoder()

encoder.encode("userpass1")
encoder.encode('userpass2')

