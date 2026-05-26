# SplitJoin for JetBrains IDEs

Toggle the construct under the caret between a single-line and a multi-line
form. Place the caret somewhere inside the expression, invoke **Split** or
**Join**, and the plugin rewrites it in place.

This is a JetBrains port of Andrew Radev's
[`splitjoin.vim`](https://github.com/AndrewRadev/splitjoin.vim) (the original
Vim plugin). The design goals are the same: a single pair of commands that
covers many constructs across many languages, with the right transformation
chosen based on the syntax node the caret is sitting in.

Built and tested against RubyMine 2024.2 and the bundled Ruby + JavaScript
plugins. Works in any JetBrains IDE that ships those plugins (IntelliJ
IDEA Ultimate, WebStorm, RubyMine, PyCharm Professional with JS, …) as well
as in Community editions for the language subsets they support.

## Actions

Two actions are registered, both under `Code` → *Split Construct* /
*Join Construct*:

| Action ID         | What it does                              |
| ----------------- | ----------------------------------------- |
| `Splitjoin.Split` | Convert single-line construct to multi-line |
| `Splitjoin.Join`  | Convert multi-line construct to single-line |

Caret position selects the construct. If no registered handler can act on the
current caret position, the document is left untouched (every handler is a
strict no-op when its preconditions aren't met).

## Keybindings

The plugin doesn't bind keys by default — pick whatever fits your muscle
memory.

### IdeaVim

Add this to `~/.ideavimrc`:

```vim
nnoremap <C-m><C-s> :action Splitjoin.Split<CR>
nnoremap <C-m><C-d> :action Splitjoin.Join<CR>
```

If you came from `splitjoin.vim` and want the original bindings:

```vim
nnoremap gS :action Splitjoin.Split<CR>
nnoremap gJ :action Splitjoin.Join<CR>
```

(`gJ` overrides Vim's built-in *join without spaces*; reassign if you rely on
it.)

### Regular JetBrains keymap

`Settings` → `Keymap` → search for *Split Construct* / *Join Construct* and
assign shortcuts. Common choices: `⌥⇧S` / `⌥⇧J` on macOS, `Alt+Shift+S` /
`Alt+Shift+J` on Linux/Windows.

## Supported transformations

Caret position in the examples below is marked with `▮`. Examples show
**Split** (`──▶`) and **Join** (`◀──`) in their natural direction; almost
all transformations are round-trippable.

### Ruby

#### Hash

```ruby
x = { a▮: 1, b: 2 }
──▶
x = {
  a: 1,
  b: 2,
}
```

#### Array

```ruby
x = [1▮, 2, 3]
──▶
x = [
  1,
  2,
  3,
]
```

#### Method call arguments (with and without parens)

```ruby
foo(a▮, b, c)
──▶
foo(a,
    b,
    c,)
```

```ruby
foo a▮: 1, b: 2
──▶
foo a: 1,
    b: 2
```

#### Method definition parameters

```ruby
def foo(a▮, b = 1, c:); end
──▶
def foo(a,
        b = 1,
        c:,); end
```

#### Block (`{ … }` ⇄ `do … end`)

```ruby
arr.map { |x▮| x + 1 }
──▶
arr.map do |x|
  x + 1
end
```

#### Lambda (arrow ⇄ `lambda do … end`)

```ruby
f = ->(x▮) { x + 1 }
──▶
f = lambda do |x|
  x + 1
end
```

#### Block-pass (`&:sym` ⇄ explicit block)

```ruby
arr.map(&:upcase▮)
──▶
arr.map { |x| x.upcase }
```

The Join direction also reverses this: `arr.map { |x| x.upcase }` →
`arr.map(&:upcase)`. It bails when the inner expression doesn't match the
simple `receiver.method` shape.

#### Method chain

```ruby
a.b▮.c.d
──▶
a
  .b
  .c
  .d
```

Bails on chains containing block calls (`.map { … }`) or parenthesized
calls (`.foo(1)`), because rewriting those would change semantics.

#### `if` / `unless` modifier

```ruby
return x if▮ y
──▶
if y
  return x
end
```

#### Ternary ⇄ `if/else`

```ruby
x = a ?▮ b : c
──▶
x = if a
      b
    else
      c
    end
```

#### `rescue` modifier ⇄ `begin/rescue/end`

```ruby
foo rescue▮ nil
──▶
begin
  foo
rescue
  nil
end
```

#### `case`/`when` with `then`

```ruby
case x; when 1 then a; when 2 then b; else c; end▮
──▶
case x
when 1
  a
when 2
  b
else
  c
end
```

#### Namespace (compact ⇄ nested)

```ruby
class Foo::Bar::B▮az < Base
  def hello; end
end
──▶
module Foo
  module Bar
    class Baz < Base
      def hello; end
    end
  end
end
```

#### `attr_accessor` / `attr_reader` / `attr_writer`

```ruby
attr_reader :a▮, :b, :c
──▶
attr_reader :a
attr_reader :b
attr_reader :c
```

#### String interpolation ⇄ concatenation

```ruby
x = "hi #{name▮}!"
──▶
x = "hi " + name.to_s + "!"
```

### JavaScript / TypeScript

Indentation in the examples assumes a 4-space style; the plugin uses the
project's configured code style for the file.

#### Object literal

```js
const x = { a▮: 1, b: 2 };
──▶
const x = {
    a: 1,
    b: 2,
};
```

Trailing commas follow the *Enforce trailing comma* setting from the JS code
style.

#### Object shorthand (Join-only)

```js
const x = { a: a▮, b: b }
◀──
const x = { a, b }
```

(`Splitjoin.Split` on `{ a: a, b: b }` delegates to the regular object
handler — multi-line, not flipped to shorthand.)

#### Array literal

```js
const arr = [1▮, 2, 3];
──▶
const arr = [
    1,
    2,
    3,
];
```

#### Call arguments

```js
foo(a▮, b, c)
──▶
foo(
    a,
    b,
    c,
)
```

#### Variable declarator list

```ts
const a = 1, b▮ = 2, c = 3
──▶
const a = 1;
const b = 2;
const c = 3;
```

#### Object / array destructuring ⇄ explicit assignments

```ts
const { a▮, b } = obj
──▶
const a = obj.a;
const b = obj.b;
```

```ts
const [a▮, b] = arr
──▶
const a = arr[0];
const b = arr[1];
```

Bails on rename (`{ a: aa }`), rest (`...rest`), or non-identifier sources;
the Join falls back to merging the declarators into a single statement when
the destructuring shape doesn't match.

#### Arrow function body (expression ⇄ block)

```ts
const f = (x▮) => x + 1;
──▶
const f = (x) => {
    return x + 1;
};
```

#### Function expression ⇄ arrow function

```ts
const f = function (x▮) { return x + 1 }
──▶
const f = (x) => x + 1
```

(Single-statement bodies switch to the concise expression form; multi-line
bodies keep the block.) Bails on generators.

#### Template literal ⇄ string concatenation

```ts
const x = `hi ${name▮}!`
──▶
const x = 'hi ' + name + '!'
```

#### Ternary ⇄ `if/else`

```ts
const x = a▮ ? b : c
──▶
let x;
if (a) { x = b; } else { x = c; }
```

Also handles assignment (`x = a ? b : c`) and `return a ? b : c`.

#### Named imports

```ts
import { a▮, b, c } from 'x'
──▶
import {
    a,
    b,
    c,
} from 'x'
```

#### JSX attribute list

```jsx
const e = <div className▮="x" id="y">hi</div>
──▶
const e = <div
    className="x"
    id="y"
>hi</div>
```

#### JSX self-close toggle

```jsx
<div▮/>
──▶
<div></div>
```

#### JSX children

```jsx
<div▮><a/><b/></div>
──▶
<div>
    <a/>
    <b/>
</div>
```

Bails on mixed text-and-element children to avoid whitespace surprises.

#### JSX fragment ⇄ `React.Fragment`

```tsx
<▮>hi</>
──▶
<React.Fragment>hi</React.Fragment>
```

### TypeScript-only

#### Type literal / interface body

```ts
type T = { a▮: number; b: string }
──▶
type T = {
    a: number;
    b: string;
}
```

Works on `interface I { … }` bodies too.

#### Union / intersection types

```ts
type T = A▮ | B | C
──▶
type T =
    | A
    | B
    | C
```

(Identical handling for `&` intersections.)

#### Tuple types

```ts
type T = [A▮, B, C]
──▶
type T = [
    A,
    B,
    C,
]
```

Preserves labels (`[a: A, b: B]`) and rest elements (`[A, ...B[]]`).

#### Generic parameter lists

```ts
type X = Foo<A▮, B, C>
──▶
type X = Foo<
    A,
    B,
    C,
>
```

Works for both call-site (`Foo<…>`) and declaration-site (`function f<…>`)
generics.

### HTML / XML

#### Tag attributes

```html
<a href▮="x" class="y">link</a>
──▶
<a
        href="x"
        class="y"
>link</a>
```

#### Tag children (element-only or single text child)

```html
<ul▮><li/><li/></ul>
──▶
<ul>
    <li/>
    <li/>
</ul>
```

#### `class=` attribute value

```html
<div class="a b c▮"/>
──▶
<div class="a
     b
     c"/>
```

#### Self-close toggle

```xml
<foo▮/>
──▶
<foo></foo>
```

In HTML, void elements (`<br>`, `<img>`, …) are excluded from the Split
direction.

### JSON

#### Object

```json
{"a▮": 1, "b": 2}
──▶
{
  "a": 1,
  "b": 2
}
```

#### Array

```json
[1▮, 2, 3]
──▶
[
  1,
  2,
  3
]
```

## Behaviour notes

- **Caret matters.** The chosen handler is whichever one's preconditions are
  satisfied at the caret's PSI node. Ambiguous positions (e.g. caret on a
  hash inside a method call) walk outward through the syntax tree.
- **No-op safety.** A handler that can't perform its transformation leaves
  the document untouched rather than corrupting it. If `Split` does nothing
  on what you expected to split, move the caret a token left or right.
- **Comments bail.** Most handlers refuse to rewrite a construct that
  contains a comment in the to-be-modified span — preserving comments
  through arbitrary rewrites is brittle, so the plugin opts out.
- **Round-trip.** `Split` followed by `Join` (or vice versa) returns the
  original text for every handler with documented examples above.

## Installation

### From source

```bash
./gradlew buildPlugin
```

The built `.zip` lands in `build/distributions/`. Install it via
`Settings` → `Plugins` → ⚙ → *Install Plugin from Disk…*

### Running a sandbox IDE

```bash
./gradlew runIde
```

## Differences from `splitjoin.vim`

This port covers the core multi-language handlers of the original, but the
implementations sit on top of the JetBrains PSI rather than on regex/textobj
inference. Practical consequences:

- Rewrites are syntactically aware, so they refuse to fire on partial or
  invalid code instead of producing garbage.
- Indentation follows the JetBrains code-style settings for the file's
  language, not the Vim plugin's hand-rolled formatting.
- Handlers that are inherently text-pattern in `splitjoin.vim` (anything
  relying on the Vim "around object" notion) are implemented here as
  PSI-walking equivalents.

Not everything from the original is ported yet — Python, Go, Perl, Lua,
Coffeescript and a handful of edge handlers are not currently included.

## License

MIT, like the original `splitjoin.vim`.
