package com.somecompany;

import com.mycompany.app.SomeGeneralTestClass;

/**
 * Hello world!
 * 
 */
public class OtherAppRequiringGeneralTestClass {

	public static void main(String[] args) throws Exception {
		SomeGeneralTestClass.class.newInstance();
	}

}
