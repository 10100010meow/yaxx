/*
* $Id: SystemContext.cpp,v 1.15 2011-08-31 08:09:21 pgr Exp $
*/

/*--------------+
| Include Files |
+--------------*/
#include <stdarg.h>
#include "SystemContext.h"
#include "ConsoleSchemeHandler.h"

#ifdef TOOLS_NAMESPACE
namespace TOOLS_NAMESPACE {
#endif

URI SystemContext::m_baseUri;
iostream * SystemContext::m_pCin = 0;
iostream * SystemContext::m_pCout = 0;
iostream * SystemContext::m_pCerr= 0;
#ifdef ANDROID
int (*SystemContext::delegatedSystemFunction)(char const *);
#endif

/*-----------------------------------------------SystemContext::SystemContext-+
|                                                                             |
+----------------------------------------------------------------------------*/
SystemContext::SystemContext(
   #ifdef ANDROID
   int (*systemFunction)(char const *),
   #endif
   char const * baseUri,
   URI::SchemeHandler sh0,
   URI::SchemeHandler sh1,
   URI::SchemeHandler sh2,
   URI::SchemeHandler sh3,
   URI::SchemeHandler sh4,
   URI::SchemeHandler sh5,
   URI::SchemeHandler sh6,
   URI::SchemeHandler sh7,
   URI::SchemeHandler sh8,
   URI::SchemeHandler sh9
) {
   #ifdef ANDROID
   delegatedSystemFunction = systemFunction;
   #endif
   RegisteredURI::registerScheme(ConsoleSchemeHandler());
   if (sh0.isPresent()) RegisteredURI::registerScheme(sh0);
   if (sh1.isPresent()) RegisteredURI::registerScheme(sh1);
   if (sh2.isPresent()) RegisteredURI::registerScheme(sh2);
   if (sh3.isPresent()) RegisteredURI::registerScheme(sh3);
   if (sh4.isPresent()) RegisteredURI::registerScheme(sh4);
   if (sh5.isPresent()) RegisteredURI::registerScheme(sh5);
   if (sh6.isPresent()) RegisteredURI::registerScheme(sh6);
   if (sh7.isPresent()) RegisteredURI::registerScheme(sh7);
   if (sh8.isPresent()) RegisteredURI::registerScheme(sh8);
   if (sh9.isPresent()) RegisteredURI::registerScheme(sh9);
   if (baseUri && strlen(baseUri)) m_baseUri = RegisteredURI(baseUri);
}

/*----------------------------------------------SystemContext::~SystemContext-+
|                                                                             |
+----------------------------------------------------------------------------*/
SystemContext::~SystemContext() {
   invalidateConsoles();
}

/*------------------------------------------------------SystemContext::system-+
|                                                                             |
+----------------------------------------------------------------------------*/
#ifdef ANDROID
int SystemContext::system(char const * command) {
   return delegatedSystemFunction(command);
}
#endif


/*------------------------------------------SystemContext::invalidateConsoles-+
|                                                                             |
+----------------------------------------------------------------------------*/
void SystemContext::invalidateConsoles()
{
   delete m_pCin;
   if (m_pCout) {
      m_pCout->flush();
      delete m_pCout;
   }
   if (m_pCerr) {
      m_pCerr->flush();
      delete m_pCerr;
   }
   m_pCin = 0;
   m_pCout = 0;
   m_pCerr = 0;
}

/*--------------------------------------------SystemContext::validateConsoles-+
|                                                                             |
+----------------------------------------------------------------------------*/
void SystemContext::validateConsoles()
{
   m_pCin = RegisteredURI(ConsoleSchemeHandler::cinUri).getStream(ios::in);
   m_pCout = RegisteredURI(ConsoleSchemeHandler::coutUri).getStream(ios::out);
   m_pCerr = RegisteredURI(ConsoleSchemeHandler::cerrUri).getStream(ios::out);
}

#ifdef TOOLS_NAMESPACE
}
#endif
/*===========================================================================*/

