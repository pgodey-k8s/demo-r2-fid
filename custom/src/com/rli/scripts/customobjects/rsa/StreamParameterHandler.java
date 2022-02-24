package com.rli.scripts.customobjects.rsa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.rsa.authn.data.AbstractParameterDTO;
import com.rsa.authn.data.FieldParameterDTO;

/**
 * Handles prompting the user for user, passcode, and PIN information.
 * Interaction with the user is simply through stdin and stdout.
 */
class StreamParameterHandler implements AuthenticatedTarget.ParameterHandler {

	/** Resource bundle used for translating prompt keys to English text */
	private final ResourceBundle promptBundle;

	/** Where we read user input from. */
	private final BufferedReader reader;

	/** Where we write user output to */
	private final PrintStream writer;

	/**
	 * Creates a new handler instance using stdin and stdout for prompting the
	 * user for information.
	 */
	public StreamParameterHandler(ResourceBundle bundle) {
		// Just wrapping stdin/stdout, so no need to worry about cleaning
		// up and closing streams.
		reader = new BufferedReader(new InputStreamReader(System.in));
		writer = new PrintStream(System.out);
		promptBundle = bundle;
	}

	/**
	 * Handles acquiring and displaying information to the user based on the
	 * supplied parameter (e.g enter new PIN). For this example, only field
	 * parameters are handled and a more robust implementation should take in to
	 * account the full range of parameters it may encounter
	 *
	 * @param parameter
	 * @throws IOException
	 */
	public void handleParameter(AbstractParameterDTO parameter)
			throws IOException {
		if (parameter instanceof FieldParameterDTO) {
			FieldParameterDTO field = (FieldParameterDTO) parameter;
			String text = promptBundle.getString(field.getPromptKey());
			if (!field.isEditable()) {
				text = MessageFormat.format(text, field.getValue());
				writer.println(text);
			} else {
				writer.println(text);
				String value = reader.readLine();
				field.setValue(value);
			}
		} else {
			throw new IllegalStateException("Unexpected parameter " + parameter);
		}
	}
}
