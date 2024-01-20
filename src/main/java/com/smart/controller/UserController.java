package com.smart.controller;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entity.Contact;
import com.smart.entity.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller

@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {

		String username = principal.getName(); // this principle will give the username of the user
//		get the user using username(in our case Email)
		User user = userRepository.getuserByUserName(username);

		model.addAttribute("user", user);
	}

	@RequestMapping("/dashboard")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "Home-user");

		return "user/dashboard";
	}

//	add contact handler

	@GetMapping("/addcontact")
	public String addContact(Model model) {
		model.addAttribute("title", "add Contact");
		model.addAttribute("contact", new Contact());
		return "user/addcontact";
	}

//	process 	add contact form
	private String generateUniqueFileName() {
		String timeStamp = String.valueOf(System.currentTimeMillis());
		String randomString = UUID.randomUUID().toString().replaceAll("-", "");

		return "image_" + timeStamp + "_" + randomString;
	}


@PostMapping("/process-contact")
public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
                             Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {

    try {
        String name = principal.getName();
        User user = userRepository.getuserByUserName(name);

        // Uploading file

        if (file.isEmpty()) {
            System.out.println("Image is not added");
            contact.setImage("contact.png");
        } else {
            // Upload the file to the folder and update the name in the contact

            // Generate a unique filename for the image
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String uniqueFileName = generateUniqueFileName() + fileExtension;

            // Save the file with the unique name using classpath-based resource handling
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:static/image/" + uniqueFileName);

            try (OutputStream outputStream = ((WritableResource) resource).getOutputStream()) {
                outputStream.write(file.getBytes());
            }
            contact.setImage(uniqueFileName);
            System.out.println("Image is added with a unique name: " + uniqueFileName);
        }

        contact.setUser(user);
        user.getContacts().add(contact);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("sessionMessage",
                new Message("Your contact is added successfully", "alert-success"));
        session.removeAttribute("message");

    } catch (Exception e) {
        System.out.println("ERROR: " + e.getMessage());
        e.printStackTrace();
        session.setAttribute("message", new Message("Something Went Wrong", "danger"));
    }

    return "redirect:/user/addcontact";
}

	@GetMapping("/viewcontacts/{page}")
	public String viewContacts(@PathVariable("page") Integer page, Model model, Principal principal) {

		model.addAttribute("title", "View Contacts");

//		sending contacts as list

		String name = principal.getName();
		User user = userRepository.getuserByUserName(name);

		// use of pageable
		// 1.currentpage
		// 2.contact per page

		Pageable pageRequest = PageRequest.of(page, 5, Sort.by("name")); // Sorting by the 'name' field
		Page<Contact> contacts = contactRepository.findContactsByUser(user.getId(), pageRequest);

		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "user/viewcontact";
	}

//	showing paricular contact in detail

	@GetMapping("/{cId}/contact")
	public String showContactInDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {

		Optional<Contact> contactOptional = contactRepository.findById(cId);

		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = userRepository.getuserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {

			model.addAttribute("title", contact.getName());

			model.addAttribute("contact", contact);
		}

		return "user/contactInDetail";
	}

//	delete particular user

	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session, Principal principal,
			RedirectAttributes redirectAttributes) {

		Optional<Contact> contactOptional = contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = userRepository.getuserByUserName(userName);
		if (user.getId() == contact.getUser().getId()) {
			contactRepository.delete(contact);
		}

		session.setAttribute("message", new Message("Contact deleted Successfully", "success"));

		redirectAttributes.addFlashAttribute("sessionMessage",
				new Message("Your contact is added successfully", "alert-success"));

		session.removeAttribute("message");

		return "redirect:/user/viewcontacts/0";
	}

//	update contact

	@GetMapping("/updateContact/{cId}")
	public String updateContact(@PathVariable("cId") Integer cId, Model model) {

		model.addAttribute("title", "UpdateContact");
		Contact contact = contactRepository.findById(cId).get();

		model.addAttribute("contact", contact);

		return "user/updateContact";
	}

//	process-update 

	@PostMapping("/process-update")
	public String processUpdateContact(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file, Model model, RedirectAttributes redirectAttributes,
			Principal principal) {

		try {

			// old contact detail

			Contact oldContactDetail = contactRepository.findById(contact.getcId()).get();

//			if user update image

			if (!file.isEmpty()) {
//				delete old photo 
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile, oldContactDetail.getImage());
				file1.delete();

				// update new photo
				String originalFileName = file.getOriginalFilename();
				String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
				String uniqueFileName = generateUniqueFileName() + fileExtension;
				// Save the file with the unique name
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + uniqueFileName);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(uniqueFileName);

			} else {
				contact.setImage(oldContactDetail.getImage());
			}

			User user = userRepository.getuserByUserName(principal.getName());
			contact.setUser(user);
			contactRepository.save(contact);
			System.out.println("Hello image db ki vachindi");
			redirectAttributes.addFlashAttribute("sessionMessage",
					new Message("Your contact is added successfully", "alert-success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("contact name" + contact.getName());

		return "redirect:/user/" + contact.getcId() + "/contact";

	}

//	user profile handler

	@GetMapping("/profile")
	public String userProfile(Model model) {
		model.addAttribute("title", "profile");

		return "user/userProfile";
	}

	// update user handler

	@GetMapping("/updateuser/{id}")
	public String updateUser(@PathVariable("id") Integer userId, Model model) {
		model.addAttribute("title", "UpdateUser");

		User user = userRepository.findById(userId).orElse(null);
		model.addAttribute("user", user);
		
		return "user/updateUser"; // Update with the correct view name for your update user page
	}

	@PostMapping("/user-process-update")
	public String processUpdateUser(@ModelAttribute User user, @RequestParam("profileImage") MultipartFile file,
			Model model, RedirectAttributes redirectAttributes, Principal principal) {

		try {
			// old user detail
			User oldUserDetail = userRepository.findById(user.getId()).get();

			// if user updates image
			if (!file.isEmpty()) {
				// delete old photo
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1 = new File(deleteFile, oldUserDetail.getImage());
				file1.delete();

				// update new photo
				String originalFileName = file.getOriginalFilename();
				String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
				String uniqueFileName = generateUniqueFileName() + fileExtension;
				// Save the file with the unique name
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + uniqueFileName);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				user.setImage(uniqueFileName);
			} else {
				user.setImage(oldUserDetail.getImage());
			}

			// Update other user details if needed

			userRepository.save(user);
			redirectAttributes.addFlashAttribute("sessionMessage",
					new Message("Your details are updated successfully", "alert-success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/user/profile"; // Change the redirect URL as needed
	}
	
	
//	settings handler
	
	@GetMapping("/settings")
	public String settings(Model model) {
		
		model.addAttribute("title", "settings-Change Password");

		return "user/settings";
	}
	
//	change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword,Principal principal,RedirectAttributes redirectAttributes,Model model) {
		
		System.out.println("oldpassword"+oldPassword);
		
		String name = principal.getName();
		User currentUser = userRepository.getuserByUserName(name);
		
		if(bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()))
		{
//			change the password
			currentUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
			userRepository.save(currentUser);
			redirectAttributes.addFlashAttribute("sessionMessage",
					new Message("Your password updated successfully", "alert-success"));
		}
		else {
			redirectAttributes.addFlashAttribute("sessionMessage",
					new Message("Please Enter correct old-password", "danger"));
			
			return "redirect:/user/settings";

		}
		
		
		//return "redirect:/user/dashboard";
		return "redirect:/signin?logout";

	}

}
