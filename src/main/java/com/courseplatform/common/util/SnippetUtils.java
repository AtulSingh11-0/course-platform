package com.courseplatform.common.util;

public final class SnippetUtils {

	private SnippetUtils () {
		// private constructor to prevent instantiation
	}

	public static String generateSnippet ( String content, String query ) {
		if ( content == null || content.isBlank() ) {
			return "";
		}

		String lowerContent = content.toLowerCase();
		String lowerQuery = query.toLowerCase();

		int index = lowerContent.indexOf(lowerQuery);
		if ( index == -1 ) {
			// query not found in content, return the start of the content
			return content.length() <= 50 ? content : content.substring(0, 50) + "...";
		}

		// determine snippet boundaries
		int snippetStart = Math.max(0, index - 10);
		int snippetEnd = Math.min(content.length(), index + lowerQuery.length() + 20);

		String snippet = content.substring(snippetStart, snippetEnd);
		if ( snippetStart > 0 ) {
			snippet = "..." + snippet;
		}
		if ( snippetEnd < content.length() ) {
			snippet = snippet + "...";
		}

		return snippet;
	}
}
