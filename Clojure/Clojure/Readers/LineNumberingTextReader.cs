﻿/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/**
 *   Author: David Miller
 **/

using System;
using System.IO;
using System.Text;

namespace clojure.lang
{
    /// <summary>
    /// 
    /// </summary>
    public class LineNumberingTextReader : PushbackTextReader, IDisposable
    {
        #region Data

        private int _lineNumber = 1;

        public int LineNumber
        {
            get { return _lineNumber; }
            set { _lineNumber = value; }
        }

        private int _prevColumnNumber = 1;

        private int _columnNumber = 1;
        
        public int ColumnNumber
        {
            get { return _columnNumber; }
        }

        private bool _prevLineStart = true;

        private bool _atLineStart = true;
        
        public bool AtLineStart
        {
            get { return _atLineStart; }
        }

        private int _index = 0;

        public int Index
        {
            get { return _index; }
        }

        private StringBuilder _sb = null;

        bool _disposed = false;

        #endregion

        #region c-tors

        public LineNumberingTextReader(TextReader reader)
            : base(reader)
        {
        }

        #endregion

        #region Basic reading

        public override int Read()
        {
            int ret = base.Read();


            _prevLineStart = _atLineStart;

            if (ret == -1)
            {
                _atLineStart = true;
                return ret;
            }

            ++_index;
            _atLineStart = false;
            ++_columnNumber;

            if (ret == '\r')
            {
                if (Peek() == '\n')
                {
                    ret = BaseReader.Read();  // TODO:  this should be base.read() okay for now with only a single character buffer, because we did a read at the top of this method, but will be wrong if we have multiple character buffer
                    ++_index;
                }
                else
                {
                    NoteLineAdvance();
                }
            }

            if ( ret == '\n' )
                NoteLineAdvance();

            if (_sb != null && ret != -1)
                _sb.Append((char)ret);

            return ret;
        }



        private void NoteLineAdvance()
        {
            _atLineStart = true;
            _lineNumber++;
            _prevColumnNumber = _columnNumber - 1;
            _columnNumber = 1;
        }

        #endregion

        #region Unreading

        public override void Unread(int ch)
        {
            base.Unread(ch);
            --_index;

            --_columnNumber;

            if (ch == '\n')
            {
                --_lineNumber;
                _columnNumber = _prevColumnNumber;
                _atLineStart = _prevLineStart;
            }

            if (_sb != null)
                _sb.Remove(_sb.Length - 1, 1);
        }

        #endregion

        #region String support

        public void CaptureString()
        {
            _sb = new StringBuilder();
        }

        public String GetString()
        {
            if (_sb == null)
                return null;

            String ret = _sb.ToString();
            _sb = null;
            return ret;
        }

        #endregion

        #region Lifetime methods

        protected override void Dispose(bool disposing)
        {
            if (!_disposed)
            {
                if (disposing)
                {
                    if (_baseReader != null)
                        _baseReader.Dispose();
                }
                _baseReader = null;
                _disposed = true;
                base.Dispose(disposing);
            }
        }


        #endregion
    }
}
