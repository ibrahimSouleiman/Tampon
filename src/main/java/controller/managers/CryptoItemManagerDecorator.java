package controller.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.Entity;

import crypt.api.annotation.ParserAction;
import crypt.api.annotation.ParserAnnotation;
import crypt.factories.ParserFactory;
import model.api.Manager;
import model.api.ManagerDecorator;
import model.api.ManagerListener;
import model.entity.Item;
import model.entity.User;

/**
 * 
 * @author Radoua Abderrahim
 *
 */
public class CryptoItemManagerDecorator extends ManagerDecorator<Item>{
	
	private User user;
	
	public CryptoItemManagerDecorator(Manager<Item> em,User user) {
		super(em);
		this.user = user;
	}
	
	@Override
	public boolean persist(Item entity) {
		
		ParserAnnotation parser = ParserFactory.createDefaultParser(entity, user);
		
		entity = (Item) parser.parseAnnotation(ParserAction.SigneAction);
		
		return super.persist(entity);
		
	}

	@Override
	public void findOneById(String id, final ManagerListener<Item> l) {
		
		super.findOneById(id,new ManagerListener<Item>() {
			
			@Override
			public void notify(Collection<Item> results) {
				
				Item item = results.iterator().next();
				
				ParserAnnotation parser = ParserFactory.createDefaultParser(item, user);
				
				item = (Item) parser.parseAnnotation(ParserAction.CheckAction);
				
				ArrayList<Item> rest = new ArrayList<>();
				
				if(item !=null){
					rest.add(item);
				}
				
				l.notify(rest);
			}
		});
	}

	@Override
	public void findAllByAttribute(String attribute, String value, final ManagerListener<Item> l) {
		
		super.findAllByAttribute(attribute, value, new ManagerListener<Item>() {
			
			@Override
			public void notify(Collection<Item> results) {
				
				ArrayList<Item> rest = new ArrayList<>();
				
				for (Iterator iterator = results.iterator(); iterator.hasNext();) {
					
					Item item = (Item) iterator.next();
					
					ParserAnnotation parser = ParserFactory.createDefaultParser(item, user);
					
					item = (Item) parser.parseAnnotation(ParserAction.CheckAction);
					
					if(item != null){
						rest.add(item);
					}
				}
				
				l.notify(rest);
			}
		});
		
	}

	@Override
	public void findOneByAttribute(String attribute, String value, final ManagerListener<Item> l) {
		
		super.findOneByAttribute(attribute, value, new ManagerListener<Item>() {
			
			@Override
			public void notify(Collection<Item> results) {
				
				Item item = results.iterator().next();
				
				ParserAnnotation parser = ParserFactory.createDefaultParser(item, user);
				
				item = (Item) parser.parseAnnotation(ParserAction.CheckAction);
				
				ArrayList<Item> rest = new ArrayList<>();
				
				if(item != null){
					rest.add(item);
				}
				
				l.notify(rest);
			}
		});
		
	}
	
	
	
}
