package com.blogspot.agilisto.classifieds.controllers;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.blogspot.agilisto.classifieds.model.Category;
import com.blogspot.agilisto.classifieds.model.Listing;
import com.blogspot.agilisto.classifieds.model.SellerIdentity;
import com.blogspot.agilisto.classifieds.services.CategoryService;
import com.blogspot.agilisto.classifieds.services.ListingService;

@Controller
public class CategoryController {
	
	@Autowired
	CategoryService categoryService;
	
	@Autowired
	ListingService listingService;

	@ResponseBody
	@RequestMapping(value = "/category", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	public void createNewCategory(@RequestParam("categoryId")String categoryId, @RequestParam("parentId")String parentId)
	{	
		Category parent = categoryService.getCategory(parentId);
		
		//Create the new category
		Category category = new Category(categoryId, parent);
		categoryService.save(category);
		
		if(parent != null)
		{
			//Add new category as a child to the parent
			List<Category> childCategories = parent.getChildren();
			childCategories.add(category);
			
			//Update parent in database
			Update update = new Update();
			update.set("children", childCategories);
			categoryService.updateCategory(parentId, update);
		}
	}
	
	@ResponseBody
	@RequestMapping(value = "/category/{categoryId}", method = RequestMethod.GET)
	public Category getCategory(@PathVariable("categoryId")String categoryId)
	{
		return categoryService.getCategory(categoryId);
	}
	
	@ResponseBody
	@RequestMapping(value = "/category/{categoryId}", method = RequestMethod.DELETE)
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteCategory(@PathVariable("categoryId")String categoryId)
	{
		Category category = categoryService.getCategory(categoryId);
		
		if(!listingService.getListings(new Query(Criteria.where("category").is(category))).isEmpty())
		{
			Logger log = Logger.getLogger(CategoryController.class.getName());
			log.warn("Category can not be deleted when it has listings associated with it");
			return;
		}
		
		if(!category.getChildren().isEmpty())
		{
			Logger log = Logger.getLogger(CategoryController.class.getName());
			log.warn("Category can not be deleted when it has children associated with it");
			return;
		}
		
		categoryService.deleteCategory(categoryId);
	}
}
