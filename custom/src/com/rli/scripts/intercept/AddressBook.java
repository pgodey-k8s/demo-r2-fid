/**
 * 
 */
package com.rli.scripts.intercept;

import java.util.Base64;

import com.rli.vds.util.InterceptParam;

/**
 * 
 */
public class AddressBook extends UserDefinedInterceptionImpl2 {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rli.scripts.intercept.UserDefinedInterceptionImpl2#authenticate(com.rli.vds.util.InterceptParam)
	 */
	public void authenticate(InterceptParam prop) {
		try {
			// get the input password
			String inputpassword = prop.getPassword();
			// encode the input password
			String encodedpassword = Base64.getEncoder().encodeToString(inputpassword
					.getBytes());

			prop.setPassword(encodedpassword);
			prop.setStatusProceed();

		} catch (Exception iex) {
			iex.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rli.scripts.intercept.UserDefinedInterceptionImpl2#insert(com.rli.vds.util.InterceptParam)
	 */
	public void insert(InterceptParam prop) {
		try {
			String command = prop.getCommand();
			// change command here if needed
			String inputpassword = "";
			String encodedpassword = "";

			String lowercommand = command.toLowerCase();
			int passwordposn = lowercommand.indexOf("password");
			int valuesposn = lowercommand.indexOf("values");

			String leftofvalues = "";
			String leftofpassword = "";
			String newtemp = "";

			if (passwordposn > 0) {
				// Count the position of the "password" field
				String left = command.substring(0, passwordposn);
				String temp = left;
				int count = 0;
				while (temp.indexOf(",") > 0) {
					count++;
					temp = temp.substring(temp.indexOf(",") + 1);
				}
				// divide the command into left and right parts with respect to
				// the values clause
				if (valuesposn > 0) {
					leftofvalues = command.substring(0, valuesposn);
					String rightofvalues = command.substring(valuesposn,
							command.length());
					int newcount = 0;
					leftofpassword = rightofvalues;
					if (count == 0) {
						newtemp = rightofvalues.substring(0, rightofvalues
								.indexOf("'"));
					} else {
						while (newcount != count) {
							newtemp = newtemp
									+ leftofpassword.substring(0,
											leftofpassword.indexOf(",") + 1);
							leftofpassword = leftofpassword
									.substring(leftofpassword.indexOf(",") + 1);
							newcount++;
						}
					}
					leftofpassword = leftofpassword.substring(leftofpassword
							.indexOf("'") + 1);
					inputpassword = leftofpassword.substring(0, leftofpassword
							.indexOf("'"));
					String rightofpassword = leftofpassword
							.substring(leftofpassword.indexOf("'"));

					// encode the input password
					encodedpassword = Base64.getEncoder().encodeToString(inputpassword
							.getBytes());
					// re-patch the encoded password in the command
					command = leftofvalues + newtemp + "'" + encodedpassword
							+ rightofpassword;
				}
			}

			prop.setCommand(command);
			prop.setStatusProceed();
			
		} catch (Exception iex) {
			iex.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rli.scripts.intercept.UserDefinedInterceptionImpl2#update(com.rli.vds.util.InterceptParam)
	 */
	public void update(InterceptParam prop) {
		try {
			// update operation is performed here; status, and errorcode
			// attributes are populated accordingly

			// for example following code returns status "proceed" with
			// errorcode "0"
			// Encoding Password Section
			String command = prop.getCommand();
			String inputpassword = "";
			String encodedpassword = "";
			String left = "";
			// String command = "UPDATE user SET userPassword='testuser1' WHERE
			// (Userid='testuser1')";
			String lowercommand = command.toLowerCase();

			int passwordposn = lowercommand.indexOf("password");
			if (passwordposn > 0) {
				left = command.substring(0, passwordposn);
				int equalposn = command.indexOf("=", passwordposn + 1);
				if (equalposn > 0) {
					int quoteposn = command.indexOf("'", equalposn + 1);
					if (quoteposn > 0) {
						int nextquoteposn = command.indexOf("'", quoteposn + 1);
						inputpassword = command.substring(quoteposn + 1,
								nextquoteposn);
						// encode the input password
						encodedpassword = Base64.getEncoder().encodeToString(inputpassword
								.getBytes());
						command = left
								+ command
										.substring(passwordposn, quoteposn + 1)
								+ encodedpassword
								+ command.substring(nextquoteposn, command
										.length());
					}
				}
			}
			
			prop.setCommand(command);
			prop.setStatusProceed();

		} catch (Exception iex) {
			iex.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see com.rli.scripts.intercept.UserDefinedInterceptionImpl2#compare(com.rli.vds.util.InterceptParam)
	 */
	@Override
	public void compare(InterceptParam prop) {
		// TODO Auto-generated method stub
		super.compare(prop);
	}

	/* (non-Javadoc)
	 * @see com.rli.scripts.intercept.UserDefinedInterceptionImpl2#delete(com.rli.vds.util.InterceptParam)
	 */
	@Override
	public void delete(InterceptParam prop) {
		// TODO Auto-generated method stub
		super.delete(prop);
	}

	/* (non-Javadoc)
	 * @see com.rli.scripts.intercept.UserDefinedInterceptionImpl2#invoke(com.rli.vds.util.InterceptParam)
	 */
	@Override
	public void invoke(InterceptParam prop) {
		// TODO Auto-generated method stub
		super.invoke(prop);
	}

	/* (non-Javadoc)
	 * @see com.rli.scripts.intercept.UserDefinedInterceptionImpl2#select(com.rli.vds.util.InterceptParam)
	 */
	@Override
	public void select(InterceptParam prop) {
		// TODO Auto-generated method stub
		super.select(prop);
	}

}
