package com.smart.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.smart.dao.UserRepository;
import com.smart.entity.User;

public class UserDetailServiceImple implements UserDetailsService{
	
	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		User user = userRepository.getuserByUserName(username);
		
		if(user == null)
		{
			throw new UsernameNotFoundException("could not found username"+username);
		}
		
		CustomUserDetails userDetails = new CustomUserDetails(user);
		
		return userDetails;
	}
	
	
}