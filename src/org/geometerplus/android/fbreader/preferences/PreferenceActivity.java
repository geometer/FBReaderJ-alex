/*
 * Copyright (C) 2009-2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.preferences;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import org.geometerplus.zlibrary.core.options.ZLIntegerOption;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.core.dialogs.ZLDialogContent;
import org.geometerplus.zlibrary.core.optionEntries.ZLColorOptionBuilder;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.zlibrary.text.view.style.*;

import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;
import org.geometerplus.zlibrary.ui.android.dialogs.ZLAndroidOptionsDialog;

import org.geometerplus.fbreader.fbreader.*;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.bookmodel.FBTextKind;

public class PreferenceActivity extends ZLPreferenceActivity {
	public PreferenceActivity() {
		super("Preferences");
	}

	@Override
	protected void init(Intent intent) {
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		//final ZLAndroidApplication androidApp = ZLAndroidApplication.Instance();

		final Category optionsCategory = createCategory(null);

		final Screen directoriesScreen = optionsCategory.createPreferenceScreen("directories");
		final Category directoriesCategory = directoriesScreen.createCategory(null);
		directoriesCategory.addPreference(new ZLStringOptionPreference(
			this, Paths.BooksDirectoryOption(),
			directoriesCategory.Resource, "books"
		));
		if (AndroidFontUtil.areExternalFontsSupported()) {
			directoriesCategory.addPreference(new ZLStringOptionPreference(
				this, Paths.FontsDirectoryOption(),
				directoriesCategory.Resource, "fonts"
			));
		}

		final Screen libraryScreen = optionsCategory.createPreferenceScreen("library");
		final Category libraryCategory = libraryScreen.createCategory(null);
		libraryCategory.addPreference(new ZLBooleanPreference(
			this, ZLAndroidApplication.Instance().NetworkLibraryEnabled,
			libraryCategory.Resource, "networkLibrary"
		));

		/*final Screen appearanceScreen = optionsCategory.createPreferenceScreen("appearance");
		final Category appearanceCategory = appearanceScreen.createCategory(null);
		appearanceCategory.addOption(androidApp.AutoOrientationOption, "autoOrientation");
		if (!androidApp.isAlwaysShowStatusBar()) {
			appearanceCategory.addOption(androidApp.ShowStatusBarOption, "showStatusBar");
		}*/

		final Screen textScreen = optionsCategory.createPreferenceScreen("text");
		final Category textCategory = textScreen.createCategory(null);
		final ZLTextStyleCollection collection = ZLTextStyleCollection.Instance();
		final ZLTextBaseStyle baseStyle = collection.getBaseStyle();
		textCategory.addPreference(new FontOption(
			this, textCategory.Resource, "font",
			baseStyle.FontFamilyOption, false
		));
		textCategory.addPreference(new ZLIntegerRangePreference(
			this, textCategory.Resource.getResource("fontSize"),
			baseStyle.FontSizeOption
		));
		textCategory.addPreference(new FontStylePreference(
			this, textCategory.Resource, "fontStyle",
			baseStyle.BoldOption, baseStyle.ItalicOption
		));
		final ZLIntegerRangeOption spaceOption = baseStyle.LineSpaceOption;
		final String[] spacings = new String[spaceOption.MaxValue - spaceOption.MinValue + 1];
		for (int i = 0; i < spacings.length; ++i) {
			final int val = spaceOption.MinValue + i;
			spacings[i] = (char)(val / 10 + '0') + "." + (char)(val % 10 + '0');
		}
		textCategory.addPreference(new ZLChoicePreference(
			this, textCategory.Resource, "lineSpacing",
			spaceOption, spacings
		));
		final String[] alignments = { "left", "right", "center", "justify" };
		textCategory.addPreference(new ZLChoicePreference(
			this, textCategory.Resource, "alignment",
			baseStyle.AlignmentOption, alignments
		));
		textCategory.addPreference(new ZLBooleanPreference(
			this, baseStyle.AutoHyphenationOption,
			textCategory.Resource, "autoHyphenations"
		));

		final ZLAndroidOptionsDialog dlg = getDialog(fbReader);
		final Screen moreStylesScreen = textCategory.createPreferenceScreen("more");
		final Category moreStylesCategory = moreStylesScreen.createCategory(null);

		byte styles[] = {
			FBTextKind.REGULAR,
			FBTextKind.TITLE,
			FBTextKind.SECTION_TITLE,
			FBTextKind.SUBTITLE,
			FBTextKind.H1,
			FBTextKind.H2,
			FBTextKind.H3,
			FBTextKind.H4,
			FBTextKind.H5,
			FBTextKind.H6,
			FBTextKind.ANNOTATION,
			FBTextKind.EPIGRAPH,
			FBTextKind.AUTHOR,
			FBTextKind.POEM_TITLE,
			FBTextKind.STANZA,
			FBTextKind.VERSE,
			FBTextKind.CITE,
			FBTextKind.INTERNAL_HYPERLINK,
			FBTextKind.EXTERNAL_HYPERLINK,
			FBTextKind.FOOTNOTE,
			FBTextKind.ITALIC,
			FBTextKind.EMPHASIS,
			FBTextKind.BOLD,
			FBTextKind.STRONG,
			FBTextKind.DEFINITION,
			FBTextKind.DEFINITION_DESCRIPTION,
			FBTextKind.PREFORMATTED,
			FBTextKind.CODE
		};
		for (int i = 0; i < styles.length; ++i) {
			final ZLTextStyleDecoration decoration = collection.getDecoration(styles[i]);
			if (decoration == null) {
				continue;
			}
			ZLTextFullStyleDecoration fullDecoration =
				decoration instanceof ZLTextFullStyleDecoration ?
					(ZLTextFullStyleDecoration)decoration : null;

			final Screen formatScreen = moreStylesCategory.createPreferenceScreen(decoration.getName());
			final Category formatCategory = formatScreen.createCategory(null);
			formatCategory.addPreference(new FontOption(
				this, textCategory.Resource, "font",
				decoration.FontFamilyOption, true
			));
			formatCategory.addPreference(new ZLIntegerRangePreference(
				this, textCategory.Resource.getResource("fontSizeDifference"),
				decoration.FontSizeDeltaOption
			));
			formatCategory.addPreference(new ZLBoolean3Preference(
				this, textCategory.Resource, "bold",
				decoration.BoldOption
			));
			formatCategory.addPreference(new ZLBoolean3Preference(
				this, textCategory.Resource, "italic",
				decoration.ItalicOption
			));
			if (fullDecoration != null) {
				final String[] allAlignments = { "unchanged", "left", "right", "center", "justify" };
				formatCategory.addPreference(new ZLChoicePreference(
					this, textCategory.Resource, "alignment",
					fullDecoration.AlignmentOption, allAlignments
				));
			}
			formatCategory.addPreference(new ZLBoolean3Preference(
				this, textCategory.Resource, "allowHyphenations",
				decoration.AllowHyphenationsOption
			));
			if (fullDecoration != null) {
				formatCategory.addPreference(new ZLIntegerRangePreference(
					this, textCategory.Resource.getResource("spaceBefore"),
					fullDecoration.SpaceBeforeOption
				));
				formatCategory.addPreference(new ZLIntegerRangePreference(
					this, textCategory.Resource.getResource("spaceAfter"),
					fullDecoration.SpaceAfterOption
				));
				formatCategory.addPreference(new ZLIntegerRangePreference(
					this, textCategory.Resource.getResource("leftIndent"),
					fullDecoration.LeftIndentOption
				));
				formatCategory.addPreference(new ZLIntegerRangePreference(
					this, textCategory.Resource.getResource("rightIndent"),
					fullDecoration.RightIndentOption
				));
				formatCategory.addPreference(new ZLIntegerRangePreference(
					this, textCategory.Resource.getResource("firstLineIndent"),
					fullDecoration.FirstLineIndentDeltaOption
				));
				final ZLIntegerOption spacePercentOption = fullDecoration.LineSpacePercentOption;
				final int[] spacingValues = new int[17];
				final String[] spacingKeys = new String[17];
				spacingValues[0] = -1;
				spacingKeys[0] = "unchanged";
				for (int j = 1; j < spacingValues.length; ++j) {
					final int val = 4 + j;
					spacingValues[j] = 10 * val;
					spacingKeys[j] = (char)(val / 10 + '0') + "." + (char)(val % 10 + '0');
				}
				formatCategory.addPreference(new ZLIntegerChoicePreference(
					this, textCategory.Resource, "lineSpacing",
					spacePercentOption, spacingValues, spacingKeys
				));
			}
				
		}

		final Screen oldColorsScreen = textCategory.createPreferenceScreen("colors");
		oldColorsScreen.setOnPreferenceClickListener(
				new PreferenceScreen.OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						dlg.run(0);
						return true;
					}
				}
		);

		final Screen colorsScreen = optionsCategory.createPreferenceScreen("colors");
		final Category colorsCategory = colorsScreen.createCategory(null);
		colorsCategory.addPreference(new ZLColorPreference(
			this, colorsCategory.Resource, "background"
		));
		colorsCategory.addPreference(new ZLColorPreference(
			this, colorsCategory.Resource, "highlighting"
		));
		colorsCategory.addPreference(new ZLColorPreference(
			this, colorsCategory.Resource, "text"
		));
		colorsCategory.addPreference(new ZLColorPreference(
			this, colorsCategory.Resource, "hyperlink"
		));

		final Screen marginsScreen = optionsCategory.createPreferenceScreen("margins");
		final Category marginsCategory = marginsScreen.createCategory(null);
		marginsCategory.addPreference(new ZLIntegerRangePreference(
			this, marginsCategory.Resource.getResource("left"),
			fbReader.LeftMarginOption
		));
		marginsCategory.addPreference(new ZLIntegerRangePreference(
			this, marginsCategory.Resource.getResource("right"),
			fbReader.RightMarginOption
		));
		marginsCategory.addPreference(new ZLIntegerRangePreference(
			this, marginsCategory.Resource.getResource("top"),
			fbReader.TopMarginOption
		));
		marginsCategory.addPreference(new ZLIntegerRangePreference(
			this, marginsCategory.Resource.getResource("bottom"),
			fbReader.BottomMarginOption
		));
	}

	private ZLAndroidOptionsDialog getDialog(FBReaderApp fbreader) {
		ZLAndroidOptionsDialog dialog = new ZLAndroidOptionsDialog(
			this,
			ZLResource.resource("dialog").getResource("OptionsDialog")
		);

		final ZLDialogContent colorsTab = dialog.createTab("Colors");
		final String colorKey = "colorFor";
		final ZLResource resource = colorsTab.getResource(colorKey);
		final ZLColorOptionBuilder builder = new ZLColorOptionBuilder();
		final String BACKGROUND = resource.getResource("background").getValue();
		final ColorProfile profile = fbreader.getColorProfile();
		builder.addOption(BACKGROUND, profile.BackgroundOption);
		builder.addOption(resource.getResource("highlighting").getValue(), profile.HighlightingOption);
		builder.addOption(resource.getResource("text").getValue(), profile.RegularTextOption);
		builder.addOption(resource.getResource("hyperlink").getValue(), profile.HyperlinkTextOption);
		builder.setInitial(BACKGROUND);
		colorsTab.addOption(colorKey, builder.comboEntry());
		colorsTab.addOption("", builder.colorEntry());

		return dialog;
	}
}
