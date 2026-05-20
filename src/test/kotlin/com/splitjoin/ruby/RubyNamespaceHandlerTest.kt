package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyNamespaceHandlerTest : BasePlatformTestCase() {

    fun `test split compact class`() {
        myFixture.configureByText(
            "a.rb",
            """
            class Foo::Bar::B<caret>az
              def hello
                "hi"
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            module Foo
              module Bar
                class Baz
                  def hello
                    "hi"
                  end
                end
              end
            end
            """.trimIndent()
        )
    }

    fun `test split compact module`() {
        myFixture.configureByText(
            "a.rb",
            """
            module Foo::B<caret>ar
              CONST = 1
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            module Foo
              module Bar
                CONST = 1
              end
            end
            """.trimIndent()
        )
    }

    fun `test split compact class with superclass`() {
        myFixture.configureByText(
            "a.rb",
            """
            class Foo::B<caret>ar < Base
              def hello
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            module Foo
              class Bar < Base
                def hello
                end
              end
            end
            """.trimIndent()
        )
    }

    fun `test join nested modules ending in class`() {
        myFixture.configureByText(
            "a.rb",
            """
            module Foo
              module Bar
                class B<caret>az
                  def hello
                  end
                end
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            """
            class Foo::Bar::Baz
              def hello
              end
            end
            """.trimIndent()
        )
    }

    fun `test join nested modules ending in class with superclass`() {
        myFixture.configureByText(
            "a.rb",
            """
            module Foo
              class B<caret>ar < Base
                def hello
                end
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            """
            class Foo::Bar < Base
              def hello
              end
            end
            """.trimIndent()
        )
    }

    fun `test join nested modules ending in module`() {
        myFixture.configureByText(
            "a.rb",
            """
            module Foo
              module B<caret>ar
                CONST = 1
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            """
            module Foo::Bar
              CONST = 1
            end
            """.trimIndent()
        )
    }

    fun `test round-trip split then join returns original`() {
        myFixture.configureByText(
            "a.rb",
            """
            class Foo::Bar::B<caret>az < Base
              def hello
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            """
            class Foo::Bar::Baz < Base
              def hello
              end
            end
            """.trimIndent()
        )
    }

    fun `test join sub-chain when outer wrapper has sibling content`() {
        myFixture.configureByText(
            "a.rb",
            """
            module Foo
              module B<caret>ar
                class Baz
                end
              end
              CONST = 1
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            "module Foo\n  class Bar::Baz\nend\n  CONST = 1\nend"
        )
    }

    fun `test join sub-chain when outer wrapper has sibling include`() {
        myFixture.configureByText(
            "a.rb",
            """
            module Foo
              include Mixin
              module B<caret>ar
                class Baz
                end
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            "module Foo\n  include Mixin\n  class Bar::Baz\nend\nend"
        )
    }

    fun `test join sub-chain when ancestor wrapper has comment`() {
        myFixture.configureByText(
            "a.rb",
            """
            module Foo
              # outer comment
              module B<caret>ar
                class Baz
                end
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            "module Foo\n  # outer comment\n  class Bar::Baz\nend\nend"
        )
    }

    fun `test single-level class with no nesting is a no-op`() {
        myFixture.configureByText(
            "a.rb",
            """
            class B<caret>az
              def hello
              end
            end
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test caret on outermost module reaches handler`() {
        myFixture.configureByText(
            "a.rb",
            """
            module F<caret>oo
              module Bar
                class Baz
                end
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            """
            class Foo::Bar::Baz
            end
            """.trimIndent()
        )
    }

    fun `test join compact chain nested inside outer with sibling content`() {
        myFixture.configureByText(
            "a.rb",
            """
            module TopLevel
              CONST = 1
              module Outer
                module Inner
                  class L<caret>eaf
                  end
                end
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            "module TopLevel\n  CONST = 1\n  class Outer::Inner::Leaf\nend\nend"
        )
    }

    fun `test caret inside innermost body still reaches handler`() {
        myFixture.configureByText(
            "a.rb",
            """
            module Foo
              module Bar
                class Baz
                  def hello
                    "hi<caret>"
                  end
                end
              end
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            """
            class Foo::Bar::Baz
              def hello
                "hi"
              end
            end
            """.trimIndent()
        )
    }

    fun `test join bails when intermediate element has compact name`() {
        myFixture.configureByText(
            "a.rb",
            """
            module Foo
              module Bar::B<caret>az
              end
            end
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }
}
